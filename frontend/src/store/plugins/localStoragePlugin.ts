import {Store} from 'vuex';
import {RootState} from '@/store/types';

const localStoragePlugin =  (store: Store<RootState>) => {
    // initialize store from local storage
    const localTools = localStorage.getItem('tools');
    const localToolsVersion = localStorage.getItem('toolsVersion');
    if (localTools && localToolsVersion) {
        store.commit('tools/setTools', JSON.parse(localTools));
        store.commit('tools/setVersion', localToolsVersion);
    }

    // save tools and version in localStorage whenever they are updated
    store.subscribe((mutation, state) => {
        if (mutation.type.startsWith('tools')) {
            localStorage.setItem('tools', JSON.stringify((state as any).tools.tools));
            localStorage.setItem('toolsVersion', (state as any).tools.version);
        }
    });
};

export default localStoragePlugin;