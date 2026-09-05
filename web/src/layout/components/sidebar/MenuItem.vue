<template>
    <!-- 按钮权限点(type=2)不参与侧边栏渲染 -->
    <el-menu-item v-if="isLeaf" :index="menu.route || String(menu.id)">
        <el-icon>
            <component :is="iconComponent" />
        </el-icon>
        <span>{{ menu.menu_name }}</span>
    </el-menu-item>
    <el-sub-menu v-else-if="!isButton" :index="menu.route || String(menu.id)">
        <template #title>
            <el-icon>
                <component :is="iconComponent" />
            </el-icon>
            <span>{{ menu.menu_name }}</span>
        </template>
        <menu-item v-for="child in childMenus" :key="child.id" :menu="child" />
    </el-sub-menu>
</template>

<script setup>
import { computed } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'

const props = defineProps({
    menu: {
        type: Object,
        required: true,
    },
})
const childMenus = computed(() => props.menu.children || [])
// 页面(type=1)渲染为可点击叶子（页面下的按钮权限点不显示，点击即路由跳转）；
// 目录(type=0)渲染为可展开子菜单；按钮(type=2)不渲染
const isLeaf = computed(() => props.menu.type === '1')
const isButton = computed(() => props.menu.type === '2')
// 按sys_menu.icon名称解析element-plus图标，未配置/不存在时缺省Menu
const iconComponent = computed(() => ElementPlusIcons[props.menu.icon] || ElementPlusIcons.Menu)
</script>
