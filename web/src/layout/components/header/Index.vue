<template>
    <div class="header">
        <div class="flex justify-center items-center">
            <el-avatar class="mr8px" :size="30" shape="circle" :src="baseUrl + userInfo.avatar" />
            <el-dropdown>
                <span>
                    {{ userInfo.name }}
                    <el-icon>
                        <arrow-down />
                    </el-icon>
                </span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item v-for="(item, i) in routerList" :divided="item.divided" :key="i" @click="goto(item.src)">
                            {{ item.name }}
                        </el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
    <el-drawer v-model="drawer" title="个人信息">
        <presonalCenter />
    </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import presonalCenter from '@/views/personal-center/Index.vue'
const baseUrl = import.meta.env.VITE_BASE_URL
const drawer = ref(false)
const router = useRouter()
// 用户信息
const userInfo = ref(JSON.parse(localStorage.getItem('userinfo')))
// 路由列表
const routerList = [
    {
        src: '/',
        name: '首页',
        divided: false,
    },
    {
        src: '/personalCenter',
        name: '个人中心',
        divided: false,
    },
    {
        src: '/login',
        name: '退出登录',
        divided: true,
    },
]

// 退出登录
const logout = (src) => {
    ElMessageBox.confirm('此操作将退出登录, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    })
        .then(() => {
            router.push(src)
            ElMessage({ message: '注销成功', type: 'success' })
            localStorage.clear()
        })
        .catch(() => {})
}

// 跳转
const goto = (src) => {
    if (src === '/login') {
        return logout(src)
    }
    if (src === '/personalCenter') {
        drawer.value = true
        return
    }
    router.push(src)
}
</script>

<style scoped>
.header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    height: 50px;
}

.el-dropdown {
    color: #fff;
    cursor: pointer;
}

:focus-visible {
    outline: none;
}
</style>
