import axios from 'axios'
import { ElMessage } from 'element-plus'
import { refreshToken as refreshTokenFn } from '@/api/auth.js'
import { showLoginMessageBox } from '@/utils/loginMessage.js'

const service = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    headers: {
        'Access-Control-Allow-Origin': '*', // 允许跨域
        'Content-Type': 'application/json', // 设置请求内容类型为 JSON
    },
    validateStatus: function (status) {
        return status >= -200 && status <= 400
    },
})

// 刷新 Token 并重试请求
const retryRequest = async (config) => {
    const token = localStorage.getItem('refreshToken')
    let result = await refreshTokenFn({ refreshToken: token })
    if (result) {
        const authorization = localStorage.getItem('authorization')
        if (authorization) {
            config.headers['authorization'] = authorization
            return service(config)
        }
    }
}

// 请求拦截器
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

// 响应拦截器
service.interceptors.response.use(
    async (response) => {
        const refreshToken = response.headers['refresh-token']
        const authorization = response.headers['authorization']
        if (authorization) localStorage.setItem('authorization', authorization)
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
        switch (response.data.code) {
            case 401:
                if (response.config.isRefreshToken) {
                    localStorage.clear()
                    await showLoginMessageBox().then(() => {
                        window.location = '/login'
                    })
                } else {
                    await retryRequest(response.config)
                }
                return
            case 403:
                return retryRequest(response.config)
            default:
                return response.data
        }
    },
    (error) => {
        if (error?.response?.status === 403) {
            localStorage.clear()
            showLoginMessageBox()
        } else {
            ElMessage({ message: error.message || error, type: 'error' })
        }
        return Promise.reject(error)
    },
)

export default service
