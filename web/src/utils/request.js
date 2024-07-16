import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { refreshToken as refreshTokenFn } from '@/api/auth.js'

const service = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    // timeout: 5000,
    headers: {
        'Access-Control-Allow-Origin': '*', // 设置允许跨域的域名，* 代表允许所有域名
        'Content-Type': 'application/json', // 设置请求的内容类型为 JSON
    },
    // `validateStatus` 定义了对于给定的 HTTP状态码是 resolve 还是 reject promise。
    // 如果 `validateStatus` 返回 `true` (或者设置为 `null` 或 `undefined`)，
    // 则promise 将会 resolved，否则是 rejected。
    validateStatus: function (status) {
        return status >= -200 || status <= 400
    },
})

/**
 * 403 拦截器
 */
const handleUnauthorized = () => {
    ElMessageBox.confirm('您还没有登录，请先登录', '提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning',
    })
        .then(() => {
            window.location = '/login'
        })
        .catch(() => {})
}

// request 拦截器
service.interceptors.request.use(
    (config) => {
        const authorization = localStorage.getItem('authorization')
        if (authorization) {
            // 每个请求携带 token
            config.headers['authorization'] = authorization
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    },
)

// response 拦截器
service.interceptors.response.use(
    async (response) => {
        const refreshToken = response.headers['refresh-token']
        const authorization = response.headers['authorization']
        if (authorization) localStorage.setItem('authorization', authorization)
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
        switch (response.data.code) {
            case 401:
                if (response.config.url === '/auth/refresh-token') {
                    handleUnauthorized()
                    localStorage.clear()
                } else {
                    const token = localStorage.getItem('refreshToken')
                    await refreshTokenFn({ refreshToken: token })
                    const authorization = localStorage.getItem('authorization')
                    if (authorization) {
                        response.config.headers['authorization'] = authorization
                        await service(response.config)
                    } else {
                        handleUnauthorized()
                        localStorage.clear()
                    }
                }
                return
            case 403:
                const token = localStorage.getItem('refreshToken')
                let r = await refreshTokenFn({ refreshToken: token })
                if (r) {
                    const authorization = localStorage.getItem('authorization')
                    if (authorization) {
                        response.config.headers['authorization'] = authorization
                        await service(response.config)
                    } else {
                        handleUnauthorized()
                        localStorage.clear()
                    }
                }
                break
            default:
                return response.data
        }
    },
    (error) => {
        if (error?.response?.status === 403) {
            localStorage.clear()
            handleUnauthorized()
        } else {
            ElMessage({ message: error, type: 'error' })
        }
        return Promise.reject(error)
    },
)

export default service
