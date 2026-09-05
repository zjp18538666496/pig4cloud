import service from '@/utils/request.js'

/**
 * 分页查询API密钥
 */
export function getApiKeyLists(data) {
    return service({ url: '/apikey/getLists', method: 'post', data })
}

/**
 * 创建密钥（返回含完整密钥，仅此一次）
 */
export function createApiKey(data) {
    return service({ url: '/apikey/create', method: 'post', data })
}

export function updateApiKey(data) {
    return service({ url: '/apikey/update', method: 'post', data })
}

export function delApiKey(data) {
    return service({ url: '/apikey/del', method: 'post', data })
}

/**
 * 调用日志分页查询（按Key/时间范围/结果过滤）
 */
export function getApiKeyLogs(data) {
    return service({ url: '/apikey/getLogs', method: 'post', data })
}
