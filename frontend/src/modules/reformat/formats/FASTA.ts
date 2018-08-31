import {Format, Sequence} from '@/modules/reformat/types';

export const FASTA: Format = {
    name: 'FASTA',

    validate(value: string): boolean {

        // remove preceding spaces and newlines
        value = value.trimLeft();

        if (value.includes('>')) { // could be one or more sequences and will have a header

            // the first real character needs to be a '>'
            if (!value.trimLeft().startsWith('>')) {
                return false;
            }

            const sequences = value.substr(1)
                .split('\n>');

            for (let sequence of sequences) {

                // remove all spaces
                sequence = sequence.replace(/ /g, '');

                // validate header
                const headerEnd: number = sequence.indexOf('\n');
                const header: string = sequence.substring(0, headerEnd);

                sequence = sequence.substr(headerEnd).replace(/\s/g, '');

                // it must start with a alphanumerical character
                if (header.length < 1 || !(/[A-Z0-9]/i).test(header[0].toUpperCase())) {
                    return false;
                }

                // if no sequence is found for header, it can't be FASTA.
                if (sequence.length < 1 || !(/[-.*A-Z]/i).test(sequence.toUpperCase())) {
                    return false;
                }
            }
            return true;

        } else { // can only be one sequence without a header
            return !(/[^-.*A-Z\n\s]/i.test(value.toUpperCase()));
        }
    },

    read(fasta: string): Sequence[] {
        const newlines = fasta.split('\n')
        // remove empty lines
            .filter((line: string) => line === '');

        const result: Sequence[] = [];

        for (let i = 0; i < newlines.length;) {
            const element: Sequence = {
                identifier: '',
                seq: '',
            };
            if (newlines[i].startsWith('>')) {
                element.identifier = newlines[i].substring(1);
                i++;
            }
            while (i < newlines.length && !newlines[i].startsWith('>')) {
                if (!newlines[i].startsWith(';')) {
                    element.seq += newlines[i];
                }
                i++;
            }
            result.push(element);
        }
        return result;
    },

    write(sequences: Sequence[]): string {
        let result = '';
        for (const sequence of sequences) {
            result += '>';
            result += sequence.identifier;
            result += '\n';
            // result += formatLongSeq(sequence.seq, 60); TODO write formatLongSeq
            // removes stars from the end of sequences, as they are specific to the pir format
            if (/\*$/.test(sequence.seq)) {
                result = result.replace(/\*$/, '');
            }
            result += '\n';
        }

        return result;
    },
};
