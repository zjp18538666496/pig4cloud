import { defineStore } from 'pinia'

/**
 * 侧边栏
 */
export const useUserInfoStore = defineStore('userInfo',  {
    state: () => {
        return {
            token: null,
            userInfo: {}
        }
    },
    //数据持久化
    persist: {
        enabled: true
    }
})
