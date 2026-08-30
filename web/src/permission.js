import router from './router'
import { ensureDynamicRoutes } from '@/router/dynamicRouter.js'
import { showLoginMessageBox } from '@/utils/loginMessage.js'

const LOGIN_PATH = '/login'

// 防止"重进导航"无限循环的标记：每次成功放行后复位
let reentered = false

// 统一路由守卫：登录态校验 + 动态路由按需注册（首次导航/刷新页面/重新登录后自动注册）
router.beforeEach(async (to) => {
    // 脏状态自愈：只剩token而用户信息缺失（登出清理与在途请求写回token竞态导致），
    // 清掉残留token引导重新登录，避免进入布局后读不到用户信息
    if (localStorage.getItem('authorization') && !localStorage.getItem('userinfo')) {
        localStorage.removeItem('authorization')
        localStorage.removeItem('refreshToken')
    }
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
    // 整页刷新场景：首次解析时动态路由尚未注册，/xxx会先被兜底规则重定向到/404；
    // 动态路由注册完成后，带原始地址重进一次导航（仅重进一次，真实不存在的路径仍落到404）
    const originalPath = to.redirectedFrom?.fullPath
    if (to.path === '/404' && originalPath && originalPath !== '/404' && !reentered) {
        reentered = true
        return originalPath
    }
    reentered = false
    return true
})
