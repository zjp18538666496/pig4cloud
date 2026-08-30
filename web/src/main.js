import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persist'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// element-plus暗黑模式css变量（html.dark生效）
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'virtual:uno.css'

import App from './App.vue'
import router from './router'
import i18n from './locales'
import { applyDark } from './utils/dark.js'
import { permission } from './directives/permission.js'
import './permission'

const app = createApp(App)
app.config.globalProperties.$baseUrl = import.meta.env.VITE_BASE_URL
app.directive('permission', permission)
// 暗黑模式按本地存储恢复
applyDark()
const pinia = createPinia()
pinia.use(piniaPersist)
app.use(pinia)
app.use(i18n)
app.use(ElementPlus, {
    locale: zhCn,
})
app.use(router)

app.mount('#app')
