import service from '@/utils/request.js'

/**
 * 分页查询操作日志
 * @param data
 * @return {*}
 */
export function getOperateLogs(data) {
    return service({
        url: '/log/getOperateLogs',
        method: 'post',
        data,
    })
}

/**
 * 分页查询登录日志
 * @param data {username, success, page, pageSize}
 * @return {*}
 */
export function getLoginLogs(data) {
    return service({
        url: '/log/getLoginLogs',
        method: 'post',
        data,
    })
}

/**
 * 导出操作日志（当前筛选，xlsx blob，上限1万行）
 */
export function exportOperateLogs(data) {
    return service({ url: '/log/exportOperateLogs', method: 'post', data, responseType: 'blob' })
}

/**
 * 导出登录日志（当前筛选，xlsx blob，上限1万行）
 */
export function exportLoginLogs(data) {
    return service({ url: '/log/exportLoginLogs', method: 'post', data, responseType: 'blob' })
}
