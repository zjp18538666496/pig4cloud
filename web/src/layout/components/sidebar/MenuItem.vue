<template>
    <el-menu-item v-if="isLeaf" :index="menu.route || String(menu.id)">
        <el-icon>
            <component :is="iconComponent" />
        </el-icon>
        <span>{{ menu.menu_name }}</span>
    </el-menu-item>
    <el-sub-menu v-else :index="menu.route || String(menu.id)">
        <template #title>
            <el-icon>
                <component :is="iconComponent" />
            </el-icon>
            {{ menu.menu_name }}
        </template>
        <!-- 页面型父节点（自身带路由与组件）：先渲染自身入口，再渲染子菜单 -->
        <el-menu-item v-if="menu.route && menu.component_path" :index="menu.route">
            <el-icon>
                <component :is="iconComponent" />
            </el-icon>
            <span>{{ menu.menu_name }}</span>
        </el-menu-item>
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
// 叶子=页面型菜单且无子菜单；有子菜单一律渲染为可展开目录（防御type误标导致的子菜单丢失）
const isLeaf = computed(() => props.menu.type === '1' && childMenus.value.length === 0)
// 按sys_menu.icon名称解析element-plus图标，未配置/不存在时缺省Menu
const iconComponent = computed(() => ElementPlusIcons[props.menu.icon] || ElementPlusIcons.Menu)
</script>
