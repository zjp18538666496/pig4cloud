import service from '@/utils/request.js'

/**
 * 回收站列表（软删除的用户/角色）
 */
export function getRecycleLists() {
    return service({ url: '/recycle/getLists', method: 'get' })
}

export function restoreRecycle(data) {
    return service({ url: '/recycle/restore', method: 'post', data })
}

export function purgeRecycle(data) {
    return service({ url: '/recycle/purge', method: 'post', data })
}
