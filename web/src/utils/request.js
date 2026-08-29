import axios from 'axios'
import { ElMessage } from 'element-plus'
import { refreshToken as refreshTokenFn } from '@/api/auth.js'
import { showLoginMessageBox } from '@/utils/loginMessage.js'

const service = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    headers: {
        'Content-Type': 'application/json', // 设置请求内容类型为 JSON
    },
})

// 刷新 Token 并重试原请求
const retryRequest = async (config) => {
    const token = localStorage.getItem('refreshToken')
    if (!token) return null
    await refreshTokenFn({ refreshToken: token })
    const authorization = localStorage.getItem('authorization')
    if (!authorization) return null
    config.headers['authorization'] = authorization
    return service(config)
}

// 请求拦截器：携带access token
service.interceptors.request.use(
    (config) => {
        const authorization = localStorage.getItem('authorization')
        if (authorization) {
            config.headers['authorization'] = authorization
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    },
)

// 响应拦截器：从响应头保存新token；业务码401时刷新token并重试
service.interceptors.response.use(
    async (response) => {
        const refreshToken = response.headers['refresh-token']
        const authorization = response.headers['authorization']
        if (authorization) localStorage.setItem('authorization', authorization)
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
        switch (response.data.code) {
            case 401:
                if (response.config.isRefreshToken) {
                    // 刷新token本身失效，跳转登录
                    localStorage.clear()
                    const isLogin = await showLoginMessageBox()
                    if (isLogin) {
                        window.location = '/login'
                    }
                } else {
                    await retryRequest(response.config)
                }
                return
            case 200:
                return response.data
            default:
                return Promise.reject(response.data)
        }
    },
    async (error) => {
        if (error?.response?.status === 401) {
            localStorage.clear()
            await showLoginMessageBox().then(() => {
                window.location = '/login'
            })
        } else {
            ElMessage({ message: error.message || error, type: 'error' })
        }
        return Promise.reject(error)
    },
)

export default service
