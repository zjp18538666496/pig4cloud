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

/**
 * 2FA绑定第一步：生成TOTP密钥与二维码 {secret, otpauthUri, qrImage}
 */
export function setup2fa() {
    return service({ url: '/auth/2fa/setup', method: 'post' })
}

/**
 * 2FA绑定第二步：首次动态码确认，返回一次性备用恢复码列表
 */
export function enable2fa(code) {
    return service({ url: '/auth/2fa/enable', method: 'post', data: { code } })
}

/**
 * 2FA解绑（需密码+动态码/备用码）
 */
export function disable2fa(data) {
    return service({ url: '/auth/2fa/disable', method: 'post', data })
}

/**
 * 当前用户2FA开启状态
 */
export function get2faStatus() {
    return service({ url: '/auth/2fa/status', method: 'get' })
}

/**
 * 重新生成备用恢复码（需验证动态码/旧备用码），旧码全部作废
 */
export function regenerateBackupCodes(code) {
    return service({ url: '/auth/2fa/backup-codes/regenerate', method: 'post', data: { code } })
}

/**
 * 超管代理登录（响应头带目标用户token，前端保存后进入代理视角）
 */
export function impersonate(username) {
    return service({ url: '/online/impersonate', method: 'post', data: { tokenJti: username } })
}
