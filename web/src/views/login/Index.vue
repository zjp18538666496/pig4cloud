<template>
    <div class="login_container">
        <div class="background">
            <img src="/public/img/background/login_bg.cbfed30c.svg" />
        </div>
        <div class="login-box">
            <div class="title" :style="brand.color ? { color: brand.color } : {}">
                <img v-if="brand.logo" :src="brand.logo" class="brand-logo" alt="logo" />
                {{ brand.name }}
            </div>
            <div class="from">
                <component :is="currentComponent" @zc="zc" @dl="dl"></component>
            </div>
        </div>
    </div>
</template>
<script setup>
import { onMounted, reactive, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import Login from '@/views/login/components/Login.vue'
import Register from '@/views/login/components/Register.vue'
import { getTenantBrand } from '@/api/tenant.js'

const currentComponent = shallowRef(Login)

// 租户品牌：URL带?tenant=租户编码时展示对应品牌（名称/logo/主题色），默认PIGX ADMIN
const route = useRoute()
const brand = reactive({ name: 'PIGX ADMIN', logo: null, color: null })
onMounted(() => {
    const tenantCode = route.query.tenant
    if (!tenantCode) return
    getTenantBrand(tenantCode).then((res) => {
        if (res?.code === 200 && res.data) {
            brand.name = res.data.name || brand.name
            brand.logo = res.data.logo
            brand.color = res.data.color
        }
    })
})
const dl = () => {
    currentComponent.value = Register
}

const zc = () => {
    currentComponent.value = Login
}
</script>
<style scoped>
@media (max-width: 768px) {
    .login_container .background {
        display: none;
    }

    .login_container .login-box {
        padding-left: 0;
        width: 100vw;
    }
}

/* 深色模式：登录页整体转暗，插画降透明度 */
:global(html.dark) .login_container {
    background-color: #0f172a;
}

:global(html.dark) .login_container .login-box .title {
    color: #e5e7eb;
}

:global(html.dark) .login_container .login-box .from {
    background: #1e293b;
    border-radius: 8px;
    padding: 16px;
}

:global(html.dark) .login_container .background {
    opacity: 0.5;
}

.login_container {
    display: flex;
    width: 100vw;
    height: 100vh;
    background-color: #f8f8f8;
    background-image: url('/public/img/background/bg.7b14eacd.png');
    background-position: center center;
    background-repeat: no-repeat;
    background-attachment: fixed;
    background-size: cover;

    .background {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        padding-right: 130px;
        width: 50vw;

        img {
            width: 500px;
        }
    }

    .login-box {
        display: flex;
        flex-direction: column;
        justify-content: center;
        padding-left: 100px;
        width: 50vw;

        .brand-logo {
            height: 36px;
            margin-right: 8px;
            vertical-align: middle;
        }

        .title {
            text-align: center;
            padding: 12px 0;
            width: 400px;
            font-weight: 600;
            font-size: 30px;
        }

        .from {
            width: 400px;
            padding: 48px;
            background-color: #fff;
            border-radius: 20px;
        }
    }
}
</style>
