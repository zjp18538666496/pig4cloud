import axios from 'axios'
import {ElMessage, ElMessageBox} from "element-plus";

const service = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    timeout: 5000,
    headers: {
        'Access-Control-Allow-Origin': '*', // 设置允许跨域的域名，* 代表允许所有域名
        'Content-Type': 'application/json', // 设置请求的内容类型为 JSON
    },
    // `validateStatus` 定义了对于给定的 HTTP状态码是 resolve 还是 reject promise。
    // 如果 `validateStatus` 返回 `true` (或者设置为 `null` 或 `undefined`)，
    // 则promise 将会 resolved，否则是 rejected。
    validateStatus: function (status) {
        return status >= -200 || status <= 400;
    },
})

/**
 * 403 拦截器
 */
const handleUnauthorized = () => {
    localStorage.removeItem('token');
    ElMessageBox.confirm('您还没有登录，请先登录', '提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
    })
        .then(() => {
            window.location = '/login'
        })
        .catch(() => {
        });
}

// request 拦截器
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            // 每个请求携带 token
            config.headers['token'] = token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// response 拦截器
service.interceptors.response.use(
    response => {
        switch (response.data.code) {
            case 401:
                handleUnauthorized()
                return
            default:
                return response.data;
        }
    },
    error => {
        if (error?.response?.status === 403) {
            localStorage.removeItem('token');
            handleUnauthorized()
        } else {
            ElMessage({message: error, type: 'error'})
        }
        return Promise.reject(error)
    }
)

export default service
