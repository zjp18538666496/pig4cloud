import service from '@/utils/request.js'

/**
 * 接口监控总览（JVM信息+接口调用统计）
 */
export function getMonitorOverview() {
    return service({ url: '/monitor/overview', method: 'get' })
}
