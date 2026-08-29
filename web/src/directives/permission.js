/**
 * 按钮级权限指令：v-permission="['user:remove']"
 * 登录响应的userinfo.permissions包含任一权限点才渲染按钮，否则移除元素
 */
const getPermissions = () => {
    try {
        const userinfo = JSON.parse(localStorage.getItem('userinfo') || '{}')
        return userinfo.permissions || []
    } catch {
        return []
    }
}

export const permission = {
    mounted(el, binding) {
        const required = binding.value
        if (!Array.isArray(required) || required.length === 0) return
        const owned = getPermissions()
        if (!required.some((item) => owned.includes(item))) {
            el.parentNode?.removeChild(el)
        }
    },
}
