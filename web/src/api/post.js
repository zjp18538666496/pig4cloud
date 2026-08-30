import service from '@/utils/request.js'

export function getPostLists(data) {
    return service({ url: '/post/getPostLists', method: 'post', data })
}

/**
 * 启用中的岗位下拉
 */
export function getEnabledPosts() {
    return service({ url: '/post/getEnabledPosts', method: 'post' })
}

export function createPost(data) {
    return service({ url: '/post/createPost', method: 'post', data })
}

export function updatePost(data) {
    return service({ url: '/post/updatePost', method: 'post', data })
}

export function delPost(data) {
    return service({ url: '/post/delPost', method: 'post', data })
}
