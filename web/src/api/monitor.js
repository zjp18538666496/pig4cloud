import service from '@/utils/request.js'

/**
 * 接口监控总览（JVM信息+接口调用统计+多实例节点聚合）
 */
export function getMonitorOverview() {
    return service({ url: '/monitor/overview', method: 'get' })
}

/**
 * 依赖健康自检（MySQL/Redis/MongoDB/FTP连通+耗时）
 */
export function getHealthDetail() {
    return service({ url: '/health/detail', method: 'get' })
}
