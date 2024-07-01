import './assets/main.css'

import {createApp} from 'vue'
import {createPinia} from 'pinia'
import piniaPersist from 'pinia-plugin-persist'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './permission'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPersist)
app.use(pinia)
app.use(ElementPlus, {
    locale: zhCn
})
app.use(router)

app.mount('#app')
