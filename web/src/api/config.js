import service from '@/utils/request.js'

/**
 * 分页查询参数配置
 */
export function getConfigLists(data) {
    return service({ url: '/config/getConfigLists', method: 'post', data })
}

export function updateConfig(data) {
    return service({ url: '/config/updateConfig', method: 'post', data })
}

/**
 * 密码策略（公开）
 */
export function getPolicy() {
    return service({ url: '/config/policy', method: 'get' })
}
