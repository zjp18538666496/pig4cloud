import service from '@/utils/request.js'

/**
 * 获取菜单列表
 * @param data
 * @return {*}
 */
export function getMenuLists(data) {
    return service({
        url: '/menu/getMenuLists',
        method: 'post',
        data,
    })
}

/**
 * 获取左侧菜单
 * @param data
 * @returns {*}
 */
export function selectMenuLists(data) {
    return service({
        url: '/menu/selectMenuLists',
        method: 'post',
        data,
    })
}

/**
 * 删除菜单
 * @param data
 * @returns {*}
 */
export function delMenu(data) {
    return service({
        url: '/menu/delMenu',
        method: 'post',
        data,
    })
}

/**
 * 更新菜单
 * @param data
 * @returns {*}
 */
export function updateMenu(data) {
    return service({
        url: '/menu/updateMenu',
        method: 'post',
        data,
    })
}

/**
 * 新增菜单
 * @param data
 * @returns {*}
 */
export function createMenu(data) {
    return service({
        url: '/menu/createMenu',
        method: 'post',
        data,
    })
}
