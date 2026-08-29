// 动态路由：菜单接口 + import.meta.glob白名单
import router from '@/router/index.js'
import { selectMenuLists } from '@/api/menu.js'

// 路由组件白名单：仅/views目录下真实存在的组件允许注册为动态路由，
// 防止数据库component_path被注入任意模块路径
const viewModules = import.meta.glob('/src/views/**/*.vue')

const resolveComponent = (componentPath) => {
    const path = componentPath?.replace(/^@\//, '/src/')
    return path ? viewModules[path] : undefined
}

// 注册状态：记录注册时使用的token，token变化(重新登录/切换账号)时重建路由
let registeredToken = null
let registeredRouteNames = []

function clearRegisteredRoutes() {
    registeredRouteNames.forEach((name) => {
        if (router.hasRoute(name)) router.removeRoute(name)
    })
    registeredRouteNames = []
    registeredToken = null
}

/**
 * 确保动态路由已按当前登录用户注册：
 * 由路由守卫在每次导航时调用；首次导航、刷新页面、重新登录后都会(重新)注册
 * @returns {Promise<boolean>} 注册是否成功
 */
export async function ensureDynamicRoutes() {
    const token = localStorage.getItem('authorization')
    if (token && registeredToken === token) return true

    const res = await selectMenuLists({ menuType: 'flatMenu' })
    if (res?.code !== 200 || !Array.isArray(res.data)) return false

    clearRegisteredRoutes()
    res.data.forEach((item) => {
        if (item.type !== '1') return
        const component = resolveComponent(item.component_path)
        if (!component) {
            console.warn(`菜单[${item.menu_name}]的组件不存在，已跳过: ${item.component_path}`)
            return
        }
        router.addRoute('Layout', {
            path: item.route,
            name: item.component_name,
            component,
        })
        registeredRouteNames.push(item.component_name)
    })
    registeredToken = token
    return true
}
