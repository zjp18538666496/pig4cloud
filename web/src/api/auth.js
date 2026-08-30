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
        isRefreshToken: true,
        data,
    })
        .then((res) => {
            return res.code === 200
        })
        .catch((err) => {
            return false
        })
}

/**
 * 登出（服务端拉黑当前token并销毁在线会话）
 */
export function logout() {
    return service({
        url: '/auth/logout',
        method: 'post',
    })
}

/**
 * 获取图形验证码
 * @return {*}
 */
export function getCaptcha() {
    return service({
        url: '/auth/captcha',
        method: 'get',
    })
}

/**
 * 发送找回密码邮箱验证码
 * @param data {email}
 * @return {*}
 */
export function sendResetCode(data) {
    return service({
        url: '/auth/sendResetCode',
        method: 'post',
        data,
    })
}

/**
 * 邮箱验证码重置密码
 * @param data {email, code, newPassword}
 * @return {*}
 */
export function resetPasswordByEmail(data) {
    return service({
        url: '/auth/resetPasswordByEmail',
        method: 'post',
        data,
    })
}
