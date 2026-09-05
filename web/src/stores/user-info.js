import { defineStore } from 'pinia'

/**
 * 侧边栏
 */
export const useUserInfoStore = defineStore('userInfo', {
    state: () => {
        return {
            token: null,
            userInfo: {},
            // 租户品牌（登录成功随LoginResult下发）：登录后侧边栏展示
            brand: null,
        }
    },
    //数据持久化
    persist: {
        enabled: true,
    },
})
