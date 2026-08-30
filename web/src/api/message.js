import service from '@/utils/request.js'

export function getMyMessages(data) {
    return service({ url: '/message/getMyMessages', method: 'post', data })
}

export function getUnreadCount() {
    return service({ url: '/message/unreadCount', method: 'get' })
}

export function markRead(id) {
    return service({ url: '/message/markRead', method: 'post', data: { id } })
}

export function markAllRead() {
    return service({ url: '/message/markAllRead', method: 'post' })
}

/**
 * 发送站内信（target_username为空则广播本租户，超管为全平台）
 */
export function sendMessage(data) {
    return service({ url: '/message/send', method: 'post', data })
}
