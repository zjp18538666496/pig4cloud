import { valiPassword, valiUsername } from '@/utils/validate.js'
class VerifyUser {
    username = (rule, value, callback) => {
        if (valiUsername(value)) {
            callback()
        } else {
            callback(new Error('请输入4到12位的用户名，支持字母合数字'))
        }
    }

    password = (rule, value, callback) => {
        if (valiPassword(value)) {
            callback()
        } else {
            callback(new Error('请输入4到18位的密码，支持字母、数字和特殊字符'))
        }
    }
}

export { VerifyUser }