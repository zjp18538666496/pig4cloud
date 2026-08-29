// 动态添加路由的函数
import router from '@/router/index.js'
import { selectMenuLists } from '@/api/menu.js'

// 路由组件白名单：仅/views目录下真实存在的组件允许注册为动态路由，
// 防止数据库component_path被注入任意模块路径
const viewModules = import.meta.glob('/src/views/**/*.vue')

const resolveComponent = (componentPath) => {
    const path = componentPath?.replace(/^@\//, '/src/')
    return path ? viewModules[path] : undefined
}

class DynamicRouter {
    async addDynamicRoutes() {
        const res = await selectMenuLists({ menuType: 'flatMenu' })
        if (res?.code !== 200 || !Array.isArray(res.data)) return

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
        })
    }
}

export default DynamicRouter
