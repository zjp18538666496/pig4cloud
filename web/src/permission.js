import router from './router'
import { ElMessageBox } from 'element-plus'

router.beforeEach(async (to, from, next) => {
    const token = localStorage.getItem('token')
    if (token) {
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
