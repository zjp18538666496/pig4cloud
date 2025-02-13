import service from '@/utils/request.js'

/**
 * 获取角色列表
 * @param data
 * @return {*}
 */
export function getRoleLists(data) {
    return service({
        url: '/role/getRoleLists',
        method: 'post',
        data,
    })
}

/**
 * 删除角色
 * @param data
 * @returns {*}
 */
export function delRole(data) {
    return service({
        url: '/role/delRole',
        method: 'post',
        data,
    })
}

/**
 * 更新角色
 * @param data
 * @returns {*}
 */
export function updateRole(data) {
    return service({
        url: '/role/updateRole',
        method: 'post',
        data,
    })
}

/**
 * 新增角色
 * @param data
 * @returns {*}
 */
export function createRole(data) {
    return service({
        url: '/role/createRole',
        method: 'post',
        data,
    })
}
