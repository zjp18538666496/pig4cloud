<template>
    <div class="sidebar">
        <div class="logo" :style="borderRight" @click="toggleCollapse">
            {{ logo }}
        </div>
        <el-menu :default-active="route.path" class="el-menu-vertical-demo" :collapse="isCollapse" :router="true">
            <el-menu-item index="/home">
                <el-icon>
                    <icon-menu />
                </el-icon>
                <span>首页</span>
            </el-menu-item>
            <menu-item v-for="menu in menuTree" :key="menu.id" :menu="menu" />
        </el-menu>
    </div>
</template>

<script setup>
import { Menu as IconMenu } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useSidebarStore } from '@/stores/sidebar.js'
import { useRoute } from 'vue-router'
import MenuItem from '@/layout/components/sidebar/MenuItem.vue'
import { selectMenuLists } from '@/api/menu.js'
import { onMounted, onUnmounted, ref } from 'vue'
import emitter from '@/utils/mitt.js'

const route = useRoute()
const store = useSidebarStore()
let { isCollapse, width, logo, borderRight } = storeToRefs(store)
const userInfo = JSON.parse(localStorage.getItem('userinfo') || '{}')
let menuTree = ref([])
const refreshMenu = () => {
    selectMenuLists({ menuType: '' }).then((res) => {
        if (res?.code === 200) {
            menuTree.value = res.data
        }
    })
}

refreshMenu()

const handleRefreshMenu = () => {
    refreshMenu()
}

onMounted(() => {
    emitter.on('refreshMenu', handleRefreshMenu)
})

onUnmounted(() => {
    emitter.off('refreshMenu', handleRefreshMenu)
})

/**
 * 折叠面板
 */
const toggleCollapse = () => {
    isCollapse.value = !isCollapse.value
    width.value = isCollapse.value ? '63px' : '200px'
    borderRight.value = isCollapse.value
        ? {
              borderRight: 'none',
          }
        : {
              borderRight: '1px solid #9a9a9a',
          }
    if (isCollapse.value) {
        logo.value = 'PIGX'
    } else {
        setTimeout(() => {
            logo.value = 'PIGX ADMIN'
        }, 500)
    }
}
</script>
<style scoped lang="scss">
.sidebar {
    display: flex;
    flex-direction: column;
    height: 100vh;

    .logo {
        width: 100%;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: #00152905 0 1px 4px;
        background-color: #2e5cf6;
        font-size: 16px;
        color: #fff;
        cursor: pointer;
        border-right: 1px solid #9a9a9a;
    }

    .el-menu {
        flex: 1;
    }
}
</style>
