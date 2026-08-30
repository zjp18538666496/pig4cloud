import service from '@/utils/request.js'

/**
 * 分页查询租户套餐
 * @param data
 * @return {*}
 */
export function getPackageLists(data) {
    return service({
        url: '/tenant/package/getPackageLists',
        method: 'post',
        data,
    })
}

/**
 * 启用中的套餐下拉（开通租户时选择）
 * @return {*}
 */
export function getEnabledPackages() {
    return service({
        url: '/tenant/package/getEnabledPackages',
        method: 'post',
    })
}

/**
 * 创建套餐
 * @param data
 * @return {*}
 */
export function createPackage(data) {
    return service({
        url: '/tenant/package/createPackage',
        method: 'post',
        data,
    })
}

/**
 * 更新套餐
 * @param data
 * @return {*}
 */
export function updatePackage(data) {
    return service({
        url: '/tenant/package/updatePackage',
        method: 'post',
        data,
    })
}

/**
 * 删除套餐
 * @param data
 * @return {*}
 */
export function delPackage(data) {
    return service({
        url: '/tenant/package/delPackage',
        method: 'post',
        data,
    })
}
