import service from '@/utils/request.js'

export function getNotifyChannels(data) {
    return service({ url: '/notify/getChannelLists', method: 'post', data })
}
export function createNotifyChannel(data) {
    return service({ url: '/notify/createChannel', method: 'post', data })
}
export function updateNotifyChannel(data) {
    return service({ url: '/notify/updateChannel', method: 'post', data })
}
export function delNotifyChannel(data) {
    return service({ url: '/notify/delChannel', method: 'post', data })
}
export function getNotifyTemplates(data) {
    return service({ url: '/notify/getTemplateLists', method: 'post', data })
}
export function createNotifyTemplate(data) {
    return service({ url: '/notify/createTemplate', method: 'post', data })
}
export function updateNotifyTemplate(data) {
    return service({ url: '/notify/updateTemplate', method: 'post', data })
}
export function delNotifyTemplate(data) {
    return service({ url: '/notify/delTemplate', method: 'post', data })
}
export function getNotifyLogs(data) {
    return service({ url: '/notify/getLogs', method: 'post', data })
}
export function testNotifySend(data) {
    return service({ url: '/notify/testSend', method: 'post', data })
}
