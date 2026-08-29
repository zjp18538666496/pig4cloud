import service from '@/utils/request.js'

/**
 * 获取租户列表
 * @param data
 * @return {*}
 */
export function getTenantLists(data) {
    return service({
        url: '/tenant/getTenantLists',
        method: 'post',
        data,
    })
}

/**
 * 开通租户
 * @param data
 * @return {*}
 */
export function createTenant(data) {
    return service({
        url: '/tenant/createTenant',
        method: 'post',
        data,
    })
}

/**
 * 更新租户（名称/状态）
 * @param data
 * @return {*}
 */
export function updateTenant(data) {
    return service({
        url: '/tenant/updateTenant',
        method: 'post',
        data,
    })
}
