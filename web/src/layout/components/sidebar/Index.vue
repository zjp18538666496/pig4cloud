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
import { useUserInfoStore } from '@/stores/user-info.js'
import { useRoute } from 'vue-router'
import MenuItem from '@/layout/components/sidebar/MenuItem.vue'
import { selectMenuLists } from '@/api/menu.js'
import { onMounted, onUnmounted, ref } from 'vue'
import emitter from '@/utils/mitt.js'

const route = useRoute()
const store = useSidebarStore()
let { isCollapse, width, logo, borderRight } = storeToRefs(store)
const userInfo = JSON.parse(localStorage.getItem('userinfo') || '{}')
// 租户品牌：租户配置了brand_name则跟随展示，默认PIGX ADMIN
const brand = useUserInfoStore().brand
const brandName = brand?.name || 'PIGX ADMIN'
const brandShort = (brand?.name || 'PIGX').slice(0, 4)
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
    // 64px与element-plus折叠态菜单宽度一致，避免菜单被裁切1px
    width.value = isCollapse.value ? '64px' : '200px'
    borderRight.value = isCollapse.value
        ? {
              borderRight: 'none',
          }
        : {
              borderRight: '1px solid #9a9a9a',
          }
    if (isCollapse.value) {
        logo.value = brandShort
    } else {
        setTimeout(() => {
            logo.value = brandName
        }, 500)
    }
}
</script>
<style scoped lang="scss">
/* 深色模式：logo底色跟随暗黑 */
:global(html.dark) .sidebar .logo {
    background-color: #1f2937;
}

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
        // 菜单树超出屏幕高度时允许上下滚动
        overflow-y: auto;
        overflow-x: hidden;

        &::-webkit-scrollbar {
            width: 4px;
        }

        &::-webkit-scrollbar-thumb {
            background: rgba(144, 147, 153, 0.4);
            border-radius: 2px;
        }
    }
}
</style>
