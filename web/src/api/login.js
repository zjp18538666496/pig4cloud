import service from '@/utils/request.js'

/**
 * 登录
 * @param data
 * @return {*}
 */
export function login(data) {
    return service({
        url: '/auth/login',
        method: 'post',
        data,
    })
}
