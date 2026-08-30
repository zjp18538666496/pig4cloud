import service from '@/utils/request.js'

/**
 * 首页仪表盘统计
 * @return {*}
 */
export function getDashboardStats() {
    return service({
        url: '/stats/dashboard',
        method: 'post',
    })
}
