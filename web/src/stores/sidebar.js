import { defineStore } from 'pinia'

/**
 * 侧边栏
 */
export const useSidebarStore = defineStore('sidebar', {
    state: () => {
        return {
            isCollapse: false,
            width: '200px',
            borderRight: {
                borderRight: '1px solid #9a9a9a',
            },
            logo: 'PIGX ADMIN',
        }
    },
    //数据持久化
    persist: {
        enabled: true,
    },
})
