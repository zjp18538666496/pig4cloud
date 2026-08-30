import service from '@/utils/request.js'

/**
 * 分页查询公告（管理列表，含草稿）
 * @param data
 * @return {*}
 */
export function getNoticeLists(data) {
    return service({
        url: '/notice/getNoticeLists',
        method: 'post',
        data,
    })
}

/**
 * 创建公告
 * @param data
 * @return {*}
 */
export function createNotice(data) {
    return service({
        url: '/notice/createNotice',
        method: 'post',
        data,
    })
}

/**
 * 更新公告
 * @param data
 * @return {*}
 */
export function updateNotice(data) {
    return service({
        url: '/notice/updateNotice',
        method: 'post',
        data,
    })
}

/**
 * 删除公告
 * @param data
 * @return {*}
 */
export function delNotice(data) {
    return service({
        url: '/notice/delNotice',
        method: 'post',
        data,
    })
}
