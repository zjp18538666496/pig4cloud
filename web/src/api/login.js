import service from '@/utils/request.js'

/**
 * 登录
 * @param data
 * @return {*}
 */
export function login(data) {
    return service({
        url: '/api/user/login',
        method: 'post',
        data,
    })
}
