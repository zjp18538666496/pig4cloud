//动态添加路由的函数
import router from '@/router/index.js'
import { selectMenuLists } from '@/api/menu.js'

class DynamicRouter {
    #dynamicRoutes = []
    #routeList = [
        {
            path: '/role-manager',
            name: 'role-manager',
            component: () => import('@/views/role-manager/Index.vue')
        },
        {
            path: '/user-manager',
            name: 'user-manager',
            component: () => import('@/views/user-manager/Index.vue')
        },
        {
            path: '/menu-manager',
            name: 'menu-manager',
            component: () => import('@/views/menu-manager/Index.vue')
        }
    ]

    constructor() {
    }

    async addDynamicRoutes() {
        await selectMenuLists({ menuType: 'flatMenu' })
            .then((res) => {
                if (res?.code === 200) {
                    return res.data
                }
            })
            .then((list) => {
                if (list) {
                    list.forEach((item) => {
                        let route = this.#routeList.find((obj) => {
                            return obj.path === item.route
                        })
                        if (route) {
                            router.addRoute('Layout', route) // 添加路由
                        }
                    })
                }
                // this.#routeList.forEach((item) => {
                //     router.addRoute('Layout', item) // 添加路由
                // })
                return Promise.resolve(list)
            })
    }
}

export default DynamicRouter

// const modules = import.meta.glob('@/views/**/*.vue')
// console.log('modules:', modules)
// const aa = [
//     {
//         id: '100102',
//         parent_id: '1001',
//         menu_name: '角色管理',
//         route: '/role-manage',
//         status: '1',
//         type: '1',
//         level: '3',
//         component: '/views/role-manager/Index.vue',
//     },
//     {
//         id: '100103',
//         parent_id: '1001',
//         menu_name: '菜单管理',
//         route: '/menu-manage',
//         status: '1',
//         type: '1',
//         level: '3',
//         component: '/views/menu-manager/Index.vue',
//     },
//     {
//         id: '100104',
//         parent_id: '1001',
//         menu_name: '用户管理',
//         route: '/user-manage',
//         status: '1',
//         type: '1',
//         level: '3',
//         component: '/views/user-manager/Index.vue',
//     },
// ]
// aa.forEach((item) => {
//     router.addRoute('Layout', {
//         path: item.route,
//         name: item.menu_name,
//         component: modules[`/src/${item.component}`],
//     })
//     console.log(`/src${item.component}`)
// })
// console.log('getRoutes:', router.getRoutes())




