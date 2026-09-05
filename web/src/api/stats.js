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

// 大屏总览（登录趋势/审批量/通知送达率）
export function getScreenSummary() {
    return service({ url: '/stats/screenSummary', method: 'get' })
}
