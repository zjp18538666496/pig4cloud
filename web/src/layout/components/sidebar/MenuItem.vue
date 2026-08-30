<template>
    <el-menu-item v-if="hasChildren" :index="menu.route">
        <el-icon>
            <component :is="iconComponent" />
        </el-icon>
        <span>{{ menu.menu_name }}</span>
    </el-menu-item>
    <el-sub-menu v-else :index="menu.route">
        <template #title>
            <el-icon>
                <component :is="iconComponent" />
            </el-icon>
            {{ menu.menu_name }}
        </template>
        <menu-item v-for="child in menu.children" :key="child.id" :menu="child" />
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
const hasChildren = props.menu.type === '1'
// 按sys_menu.icon名称解析element-plus图标，未配置/不存在时缺省Menu
const iconComponent = computed(() => ElementPlusIcons[props.menu.icon] || ElementPlusIcons.Menu)
</script>
