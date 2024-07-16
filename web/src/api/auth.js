import service from '@/utils/request.js'

/**
 * 刷新token
 * @param data
 * @return {*}
 */
export function refreshToken(data) {
    return service({
        url: '/auth/refresh-token',
        method: 'post',
        data,
    })
        .then((res) => {
            return res.code === 200
        })
        .catch((err) => {
            return false
        })
}
