import router from './router'
import { ensureDynamicRoutes } from '@/router/dynamicRouter.js'
import { showLoginMessageBox } from '@/utils/loginMessage.js'

const LOGIN_PATH = '/login'

// 统一路由守卫：登录态校验 + 动态路由按需注册（首次导航/刷新页面/重新登录后自动注册）
router.beforeEach(async (to) => {
    const authorization = localStorage.getItem('authorization')

    // 登录页：已登录则回首页
    if (to.path === LOGIN_PATH) {
        return authorization ? '/' : true
    }

    // 未登录：弹框引导去登录
    if (!authorization) {
        const isLogin = await showLoginMessageBox()
        return isLogin ? LOGIN_PATH : false
    }

    // 已登录：动态路由未注册（或登录用户已变）时先注册再放行
    const registered = await ensureDynamicRoutes()
    if (!registered) {
        // 菜单拉取失败：token失效时request.js已引导重新登录，这里中断本次导航
        return false
    }
    return true
})
