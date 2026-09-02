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

/**
 * 数据大屏：租户维度使用报表（超管）
 */
export function getTenantReport() {
    return service({ url: '/stats/tenantReport', method: 'get' })
}
