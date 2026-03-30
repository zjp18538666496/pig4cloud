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
    // 打包后文件目录
    base: './',
})
