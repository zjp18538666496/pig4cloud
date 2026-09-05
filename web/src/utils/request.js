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

// 登录跳转单飞：清理后跳转登录前，在途请求的响应不再写回token、不再重复弹框
let redirectingToLogin = false
const clearAndGoLogin = async () => {
    if (redirectingToLogin) return
    redirectingToLogin = true
    localStorage.removeItem('authorization')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userinfo')
    const isLogin = await showLoginMessageBox()
    if (isLogin) {
        window.location = '/login'
    }
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
        window.dispatchEvent(new window.CustomEvent('pigx:server-ok'))
        switch (response.data.code) {
            case 401:
                if (response.config.isRefreshToken) {
                    // 刷新token本身失效，跳转登录
                    await clearAndGoLogin()
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
            await clearAndGoLogin()
        } else if (!error?.response || !error.response.data?.code) {
            // 网络/服务不可达（代理500空体等）：不逐条弹错，发全局断连事件由横幅统一提示
            window.dispatchEvent(new window.CustomEvent('pigx:server-error'))
            return Promise.reject({ ...error, silent: true, message: '后端服务连接失败' })
        } else {
            ElMessage({ message: error.message || error, type: 'error' })
        }
        return Promise.reject(error)
    },
)

export default service
