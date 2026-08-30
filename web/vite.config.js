import { fileURLToPath, URL } from 'node:url'
import UnoCSS from 'unocss/vite'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        vue(),
        UnoCSS(),
        vueDevTools(),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    build: {
        target: 'esnext', // 使用 esnext 以支持最新特性
    },
    server: {
        proxy: {
            // 站内信WebSocket实时推送（/ws/{token}）
            '/ws': {
                target: 'http://127.0.0.1:9000',
                ws: true,
            },
            // 后端所有接口统一挂/api前缀，开发环境代理到9000端口
            '/api': {
                target: 'http://127.0.0.1:9000',
                changeOrigin: true,
            },
        },
    },
    // 打包后文件目录
    base: './',
})
