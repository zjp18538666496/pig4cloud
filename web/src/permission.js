import router from './router'
import DynamicRouter from '@/router/dynamicRouter.js'
import { showLoginMessageBox } from '@/utils/loginMessage.js'

let authorization = localStorage.getItem('authorization')
//加载动态路由
if (authorization) {
    await new DynamicRouter().addDynamicRoutes()
}
await router.beforeEach(async (to, from, next) => {
    authorization = localStorage.getItem('authorization')
    if (authorization) {
        if (to.path === '/login') {
            next('/')
        } else {
            next()
        }
    } else if (to.path !== '/login') {
        localStorage.clear()
        await showLoginMessageBox
            .then(() => {
                next('/login')
            })
            .catch(() => {
                next(false)
            })
    } else {
        next()
    }
})
