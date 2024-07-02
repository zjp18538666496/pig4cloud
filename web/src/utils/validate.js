/**
 * 校验用户名 4-12位字母数字组合
 * @param value
 * @return {boolean}
 */
export function valiUsername(value) {
    return /^[a-zA-Z0-9]{4,12}$/.test(value)
}

/**
 * 校验密码 4-18位字母数字组合
 * @param value
 * @return {boolean}
 */
export function valiPassword(value) {
    return /^[a-zA-Z0-9!@#$%^&*(),.?":{}|<>~`\\\/\[\]\-_+=;']{4,18}$/.test(value)
}