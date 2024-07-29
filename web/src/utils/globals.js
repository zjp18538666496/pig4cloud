import { getCurrentInstance } from 'vue'

export function useGlobals() {
    const instance = getCurrentInstance()
    if (instance) {
        const globalProperties = instance.appContext.config.globalProperties
        return {
            baseUrl: globalProperties.$baseUrl,
        }
    }
    return {
        baseUrl: '',
    }
}
