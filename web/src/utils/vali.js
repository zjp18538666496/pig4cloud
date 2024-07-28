import { valiPassword, valiUsername } from '@/utils/validate.js'
class VerifyUser {
    username = (rule, value, callback) => {
        if (valiUsername(value)) {
            callback()
        } else {
            callback(new Error('请输入4到12位的用户名，支持字母合数字'))
        }
    }

    password = (rule, value, callback ) => {
        if (valiPassword(value)) {
            callback()
        } else {
            callback(new Error('请输入4到18位的密码，支持字母、数字和特殊字符'))
        }
    }

    newPassword = (rule, value, callback, source) => {
        const { password } = source;
        if (valiPassword(value)) {
            if (value === password) {
                callback(new Error('新密码不能与旧密码相同'));
            } else {
                callback();
            }
        } else {
            callback(new Error('请输入4到18位的密码，支持字母、数字和特殊字符'));
        }
    }

    confirmPassword = (rule, value, callback, source) => {
        const { newPassword } = source;
        if (valiPassword(value)) {
            if (value === newPassword) {
                callback();
            } else {
                callback(new Error('确认密码与新密码不一致'));
            }
        } else {
            callback(new Error('请输入4到18位的密码，支持字母、数字和特殊字符'));
        }
    }
}

export { VerifyUser }