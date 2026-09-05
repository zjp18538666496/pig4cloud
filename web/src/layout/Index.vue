<template>
    <div class="common-layout">
        <el-container>
            <el-aside :width="width">
                <Sidebar />
            </el-aside>
            <el-container>
                <el-header>
                    <Header />
                </el-header>
                <TabsBar />
                <el-main class="app_main">
                    <AppMain />
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>

<script setup>
import Sidebar from '@/layout/components/sidebar/Index.vue'
import AppMain from '@/layout/components/AppMain.vue'
import Header from '@/layout/components/header/Index.vue'
import TabsBar from '@/layout/components/tabs/Index.vue'
import { storeToRefs } from 'pinia'
import { useSidebarStore } from '@/stores/sidebar.js'
import { onMounted, onUnmounted } from 'vue'

const store = useSidebarStore()
let { width } = storeToRefs(store)

// 移动端响应式：窄屏自动折叠侧边栏（只显示图标），宽屏不自动展开
const MOBILE_BREAKPOINT = 768
const handleWindowResize = () => {
    if (window.innerWidth <= MOBILE_BREAKPOINT && !store.isCollapse) {
        store.isCollapse = true
        store.width = '64px'
        store.borderRight = { borderRight: 'none' }
        store.logo = (store.logo || 'PIGX ADMIN').slice(0, 4)
    }
}
onMounted(() => {
    handleWindowResize()
    window.addEventListener('resize', handleWindowResize)
})
onUnmounted(() => window.removeEventListener('resize', handleWindowResize))
</script>
<style scoped>
.el-header {
    height: 50px;
    background-color: #2e5cf6;
}

.el-aside {
    overflow: hidden;
    transition: width 0.3s ease-in-out;
}

@media (max-width: 768px) {
    .app_main {
        padding: 8px;
    }
}

.app_main {
    padding: 15px;
    width: 100%;
    height: calc(100vh - 84px);
    background-color: rgb(248, 248, 248);
    overflow: hidden;
}

.common-layout {
    width: 100vw;
    height: 100vh;
    overflow: hidden;
}
</style>
