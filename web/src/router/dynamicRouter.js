//动态添加路由的函数
import router from '@/router/index.js'
import { selectMenuLists } from '@/api/menu.js'

class DynamicRouter {
    constructor() {}
    async addDynamicRoutes() {
        await selectMenuLists({ menuType: 'flatMenu' })
            .then((res) => {
                if (res?.code === 200) {
                    return res.data
                }
            })
            .then((list) => {
                list?.forEach((item) => {
                    if(item.type === '1') {
                        const realPath = item.component_path?.replace(/^@\//, '/src/') // 替换 @/ 为实际路径
                        router.addRoute('Layout', {
                            path: item.route,
                            name: item.component_name,
                            component: () => import(realPath),
                        }) // 添加路由
                    }
                })
                //用于没有菜单时显示默认菜单
                // let routeList = [
                //     {
                //         path: '/role-manager',
                //         name: 'role-manager',
                //         component: () => import('@/views/role-manager/Index.vue')
                //     },
                //     {
                //         path: '/user-manager',
                //         name: 'user-manager',
                //         component: () => import('@/views/user-manager/Index.vue')
                //     },
                //     {
                //         path: '/menu-manager',
                //         name: 'menu-manager',
                //         component: () => import('@/views/menu-manager/Index.vue')
                //     }
                // ]
                // routeList.forEach((item) => {
                //     router.addRoute('Layout', item) // 添加路由
                // })
                return Promise.resolve(list)
            })
    }
}

export default DynamicRouter
