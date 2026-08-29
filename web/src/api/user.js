import service from '@/utils/request.js'

/**
 * 查询用户列表
 * @param id
 * @return {*}
 */
export function getUser(id) {
    return service({
        url: '/user/id/' + id,
        method: 'get',
    })
}

/**
 * 注册账号（无需登录）
 * @param data
 * @return {*}
 */
export function register(data) {
    return service({
        url: '/user/register',
        method: 'POST',
        data,
    })
}

/**
 * 创建用户（用户管理，需要user:write权限）
 * @param data
 * @return {*}
 */
export function createUser(data) {
    return service({
        url: '/user/createUser',
        method: 'POST',
        data,
    })
}

/**
 * 更新用户信息
 * @param data
 * @return {*}
 */
export function updateUser(data) {
    return service({
        url: '/user/updateUser',
        method: 'POST',
        data,
    })
}

/**
 * 删除用户
 * @param data
 * @return {*}
 */
export function delUser(data) {
    return service({
        url: '/user/delUser',
        method: 'POST',
        data,
    })
}

/**
 * 获取用户列表
 * @param data
 * @returns {*}
 */
export function getUserLists(data) {
    return service({
        url: '/user/getUserList',
        method: 'POST',
        data,
    })
}

/**
 * 更新用户密码
 * @param data
 * @returns {*}
 */
export function updatePassword(data) {
    return service({
        url: '/user/updatePassword',
        method: 'POST',
        data,
    })
}
