import service from '@/utils/request.js'

/**
 * 我的登录历史
 */
export function getMyLoginLogs(params) {
    return service({ url: '/profile/loginLogs', method: 'get', params })
}

/**
 * 我的操作记录
 */
export function getMyOperateLogs(params) {
    return service({ url: '/profile/operateLogs', method: 'get', params })
}

/**
 * 我的登录设备（isCurrent标记当前设备）
 */
export function getMySessions() {
    return service({ url: '/profile/sessions', method: 'get' })
}

/**
 * 踢掉自己的其它设备
 */
export function kickMySession(data) {
    return service({ url: '/profile/sessions/kick', method: 'post', data })
}
