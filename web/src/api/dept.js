import service from '@/utils/request.js'

/**
 * 获取部门树（超管可传 tenant_id 只看指定租户，不传看全部）
 * @param data {tenant_id}
 * @return {*}
 */
export function getDeptTree(data = {}) {
    return service({
        url: '/dept/getDeptTree',
        method: 'post',
        data,
    })
}

/**
 * 创建部门
 * @param data
 * @return {*}
 */
export function createDept(data) {
    return service({
        url: '/dept/createDept',
        method: 'post',
        data,
    })
}

/**
 * 更新部门
 * @param data
 * @return {*}
 */
export function updateDept(data) {
    return service({
        url: '/dept/updateDept',
        method: 'post',
        data,
    })
}

/**
 * 删除部门（级联删除子部门）
 * @param data
 * @return {*}
 */
export function delDept(data) {
    return service({
        url: '/dept/delDept',
        method: 'post',
        data,
    })
}
