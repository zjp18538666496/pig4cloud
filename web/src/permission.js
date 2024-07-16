import router from './router'
import { ElMessageBox } from 'element-plus'
import DynamicRouter from '@/router/dynamicRouter.js'
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
        await ElMessageBox.confirm('您还没有登录，请先登录', '提示', {
            confirmButtonText: '去登录',
            cancelButtonText: '取消',
            type: 'warning',
        })
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
