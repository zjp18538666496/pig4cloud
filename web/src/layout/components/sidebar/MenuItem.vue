<template>
    <el-menu-item v-if="hasChildren" :index="menu.route">
        <el-icon>
            <icon-menu />
        </el-icon>
        <span>{{ menu.menu_name }}</span>
    </el-menu-item>
    <el-sub-menu v-else :index="menu.route">
        <template #title>
            <el-icon>
                <icon-menu />
            </el-icon>
            {{ menu.menu_name }}
        </template>
        <menu-item v-for="child in menu.children" :key="child.id" :menu="child" />
    </el-sub-menu>
</template>

<script setup>
import { Menu as IconMenu } from '@element-plus/icons-vue'

const props = defineProps({
    menu: {
        type: Object,
        required: true,
    },
})
const hasChildren = props.menu.type === '1'
</script>
