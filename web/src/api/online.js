import service from '@/utils/request.js'

/**
 * 分页查询在线用户会话
 * @param data {username, page, pageSize}
 * @return {*}
 */
export function getOnlineUsers(data) {
    return service({
        url: '/online/getOnlineUsers',
        method: 'post',
        data,
    })
}

/**
 * 强制下线
 * @param data {tokenJti}
 * @return {*}
 */
export function kickOut(data) {
    return service({
        url: '/online/kickOut',
        method: 'post',
        data,
    })
}
