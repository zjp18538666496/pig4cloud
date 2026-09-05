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
 * 公开接口：按租户编码查品牌（登录页展示，无需登录）
 */
export function getTenantBrand(tenantCode) {
    return service({
        url: '/tenant/brand',
        method: 'get',
        params: { tenantCode },
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

/**
 * 删除租户（敏感操作：headers需携带X-Reauth-Password做二次认证）
 */
export function delTenant(data, headers) {
    return service({
        url: '/tenant/delTenant',
        method: 'post',
        data,
        headers,
    })
}
