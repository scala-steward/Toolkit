<template>
    <Loading :message="$t('loading')"
             v-if="loading"/>
    <div v-else
         class="font-small">
        <b v-if="total === 0"
           v-text="$t('jobs.results.hmmer.noResults')">
        </b>
        <div v-else>
            <div class="result-options">
                <a @click="scrollTo('visualization')">{{$t('jobs.results.hitlist.visLink')}}</a>
                <a @click="scrollTo('hits')">{{$t('jobs.results.hitlist.hitsLink')}}</a>
                <a @click="scrollTo('alignments')"
                   class="mr-4">{{$t('jobs.results.hitlist.alnLink')}}</a>
                <a class="border-right mr-4"></a>
                <a @click="toggleAllSelected" :class="{active: allSelected}">
                    {{$t('jobs.results.actions.selectAll')}}</a>
                <a @click="forward(false)">{{$t('jobs.results.actions.forward')}}</a>
                <a @click="toggleColor"
                   :class="{active: color}">{{$t('jobs.results.actions.colorSeqs')}}</a>
                <a @click="toggleWrap"
                   :class="{active: wrap}">{{$t('jobs.results.actions.wrapSeqs')}}</a>
            </div>

            <div v-html="$t('jobs.results.hmmer.numHits', {num: total})"></div>

            <div v-if="info.coil === '0' || info.tm === '1' || info.signal === '1'" class="mt-2">
                Detected sequence features:
                <b v-if="info.coil === '0'" v-html="$t('jobs.results.sequenceFeatures.coil')"></b>
                <b v-if="info.tm > '1'" v-html="$t('jobs.results.sequenceFeatures.tm')"></b>
                <b v-if="info.signal === '1'" v-html="$t('jobs.results.sequenceFeatures.signal')"></b>
            </div>

            <div class="result-section"
                 ref="visualization">
                <h4>{{$t('jobs.results.hitlist.vis')}}</h4>
                <hit-map :job="job"
                         @elem-clicked="scrollToElem"
                         @resubmit-section="resubmitSection"/>
            </div>

            <div class="result-section"
                 ref="hits">
                <h4 class="mb-4">{{$t('jobs.results.hitlist.hits')}}</h4>
                <hit-list-table :job="job"
                                :fields="hitListFields"
                                @elem-clicked="scrollToElem"
                                :selected-items="selectedItems"/>
            </div>

            <div class="result-section"
                 ref="alignments">
                <h4>{{$t('jobs.results.hitlist.aln')}}</h4>

                <div class="table-responsive"
                     ref="scrollElem">
                    <table class="alignments-table">
                        <tbody>
                        <template v-for="(al, i) in alignments">
                            <tr class="blank-row"
                                :key="'alignment-' + al.num"
                                :ref="'alignment-' + al.num">
                                <td colspan="4">
                                    <hr v-if="i !== 0"/>
                                </td>
                            </tr>
                            <tr>
                                <td></td>
                                <td colspan="3"
                                    v-html="al.fastaLink">
                                </td>
                            </tr>
                            <tr class="font-weight-bold">
                                <td class="no-wrap">
                                    <b-checkbox @change="check($event, al.num)"
                                                class="d-inline"
                                                :checked="selectedItems.includes(al.num)"/>
                                    <span v-text="al.num + '.'"></span>
                                </td>
                                <td colspan="3"
                                    v-html="al.acc + ' ' + al.name"></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td colspan="3"
                                    v-html="$t('jobs.results.hmmer.alignmentInfo', al)"></td>
                            </tr>

                            <template v-for="alPart in wrapAlignments(al)">
                                <tr class="blank-row">
                                    <td></td>
                                </tr>
                                <tr v-if="alPart.query.seq"
                                    class="sequence">
                                    <td></td>
                                    <td>Q</td>
                                    <td v-text="alPart.query.start"></td>
                                    <td v-html="coloredSeq(alPart.query.seq) + alEnd(alPart.query)"></td>
                                </tr>
                                <tr v-if="alPart.agree"
                                    class="sequence">
                                    <td></td>
                                    <td></td>
                                    <td></td>
                                    <td v-text="alPart.agree"></td>
                                </tr>
                                <tr v-if="alPart.template.seq"
                                    class="sequence">
                                    <td></td>
                                    <td>T</td>
                                    <td v-text="alPart.template.start"></td>
                                    <td v-html="coloredSeq(alPart.template.seq) + alEnd(alPart.template)"></td>
                                </tr>
                            </template>

                        </template>

                        <tr v-if="alignments.length !== total">
                            <td colspan="4">
                                <Loading :message="$t('jobs.results.alignment.loadingHits')"
                                         v-if="loadingMore"
                                         justify="center"
                                         class="mt-4"/>
                                <intersection-observer @intersect="intersected"/>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
    import mixins from 'vue-typed-mixins';
    import Loading from '@/components/utils/Loading.vue';
    import Logger from 'js-logger';
    import HitListTable from '@/components/jobs/result-tabs/sections/HitListTable.vue';
    import HitMap from '@/components/jobs/result-tabs/sections/HitMap.vue';
    import IntersectionObserver from '@/components/utils/IntersectionObserver.vue';
    import {HMMERAlignmentItem, HMMERHHInfoResult, SearchAlignmentItemRender} from '@/types/toolkit/results';
    import SearchResultTabMixin from '@/mixins/SearchResultTabMixin';

    const logger = Logger.get('HmmerResultsTab');

    export default mixins(SearchResultTabMixin).extend({
        name: 'HmmerResultsTab',
        components: {
            Loading,
            HitListTable,
            HitMap,
            IntersectionObserver,
        },
        data() {
            return {
                alignments: undefined as HMMERAlignmentItem[] | undefined,
                info: undefined as HMMERHHInfoResult | undefined,
                breakAfter: 90,
                hitListFields: [{
                    key: 'numCheck',
                    label: this.$t('jobs.results.hmmer.table.num'),
                    sortable: true,
                }, {
                    key: 'acc',
                    label: this.$t('jobs.results.hmmer.table.accession'),
                    sortable: true,
                }, {
                    key: 'name',
                    label: this.$t('jobs.results.hmmer.table.description'),
                    sortable: true,
                }, {
                    key: 'fullEval',
                    label: this.$t('jobs.results.hmmer.table.full_evalue'),
                    class: 'no-wrap',
                    sortable: true,
                }, {
                    key: 'eval',
                    label: this.$t('jobs.results.hmmer.table.eValue'),
                    class: 'no-wrap',
                    sortable: true,
                }, {
                    key: 'bitScore',
                    label: this.$t('jobs.results.hmmer.table.bitscore'),
                    sortable: true,
                }, {
                    key: 'hitLen',
                    label: this.$t('jobs.results.hmmer.table.hit_len'),
                    sortable: true,
                }],
            };
        },
        methods: {
            wrapAlignments(al: HMMERAlignmentItem): SearchAlignmentItemRender[] {
                if (this.wrap) {
                    const res: SearchAlignmentItemRender[] = [];
                    let qStart: number = al.query.start;
                    let tStart: number = al.template.start;
                    for (let start = 0; start < al.query.seq.length; start += this.breakAfter) {
                        const end: number = start + this.breakAfter;
                        const qSeq: string = al.query.seq.slice(start, end);
                        const tSeq: string = al.template.seq.slice(start, end);
                        const qEnd: number = qStart + qSeq.length - (qSeq.match(/[-.]/g) || []).length - 1;
                        const tEnd: number = tStart + tSeq.length - (tSeq.match(/[-.]/g) || []).length - 1;
                        res.push({
                            agree: al.agree.slice(start, end),
                            query: {
                                end: qEnd,
                                seq: qSeq,
                                start: qStart,
                            },
                            template: {
                                end: tEnd,
                                seq: tSeq,
                                start: tStart,
                            },
                        });
                        qStart = qEnd + 1;
                        tStart = tEnd + 1;
                    }
                    return res;
                } else {
                    return [al];
                }
            },
        },
    });
</script>

<style lang="scss" scoped>
    .result-section {
        padding-top: 3.5rem;
    }

    .alignments-table {
        font-size: 0.95em;

        .blank-row {
            height: 0.8rem;
        }

        .sequence td {
            word-break: keep-all;
            white-space: nowrap;
            font-family: $font-family-monospace;
            padding: 0 1rem 0 0;
        }

        a {
            cursor: pointer;
            color: $primary;

            &:hover {
                color: $tk-dark-green;
            }
        }
    }
</style>