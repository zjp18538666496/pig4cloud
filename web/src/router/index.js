import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/Index.vue'
import Home from '@/views/home/Index.vue'
import Login from '@/views/login/Index.vue'
import errorPage from '@/views/error-page/404/Index.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Layout',
            component: Layout,
            redirect: 'home',
            children: [
                {
                    path: '/home',
                    name: 'home',
                    component: Home,
                },
            ],
        },
        {
            path: '/login',
            name: '登录',
            component: Login,
        },
        // {
        //     path: '/:catchAll(.*)',
        //     name: '*',
        //     redirect: '/404',
        // },
        // {
        //     path: '/404',
        //     name: '/404',
        //     component: errorPage,
        //     hidden: true,
        // },
    ],
})

export default router
