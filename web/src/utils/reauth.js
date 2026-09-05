import { ElMessageBox } from 'element-plus'

/**
 * 敏感操作二次认证：弹窗要求输入当前登录密码，返回请求头对象。
 * 取消时抛出cancel，调用方catch跳过即可
 */
export function confirmReauth(tip = '该操作为敏感操作，请输入当前登录账号的密码确认') {
    return ElMessageBox.prompt(tip, '二次认证', {
        inputType: 'password',
        inputPlaceholder: '当前登录账号密码',
        confirmButtonText: '确认执行',
        cancelButtonText: '取消',
    }).then(({ value }) => {
        if (!value) throw 'cancel'
        return { 'X-Reauth-Password': value }
    })
}
