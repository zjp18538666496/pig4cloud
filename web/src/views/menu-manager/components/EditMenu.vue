<template>
    <el-form ref="ruleFormRef" :model="props.menuInfo" :rules="rules" label-width="auto" class="max-w600px">
        <el-form-item prop="menu_name" label="上级菜单">
            <el-tree-select v-model="terrNode" :data="menuList" :render-after-expand="false" :props="defaultProps" :check-strictly="true" @current-change="currentChange"></el-tree-select>
        </el-form-item>
        <el-form-item prop="menu_name" label="菜单名称">
            <el-input v-model="props.menuInfo.menu_name" />
        </el-form-item>
        <el-form-item prop="route" label="路由地址">
            <el-input v-model="props.menuInfo.route" />
        </el-form-item>
        <el-form-item prop="route" label="组件名称">
            <el-input :disabled="props.menuInfo.type !== '1'" v-model="props.menuInfo.component_name" />
        </el-form-item>
        <el-form-item prop="route" label="组件地址">
            <el-input :disabled="props.menuInfo.type !== '1'" v-model="props.menuInfo.component_path" />
        </el-form-item>
        <el-form-item prop="status" label="状态">
            <el-radio-group v-model="props.menuInfo.status">
                <el-radio value="0" size="large">禁用</el-radio>
                <el-radio value="1" size="large">启用</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item prop="type" label="菜单类型">
            <el-radio-group @change="changeState(props.menuInfo.type)" v-model="props.menuInfo.type">
                <el-radio value="0" size="large">目录</el-radio>
                <el-radio value="1" size="large">菜单</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item prop="level" label="层级">
            <el-input v-model="props.menuInfo.level" disabled />
        </el-form-item>
    </el-form>
</template>

<script setup>
import { ref } from 'vue'
import { getMenuLists } from '@/api/menu.js'

const props = defineProps({
    menuInfo: {
        type: Object,
        required: true,
    },
    rules: {
        type: Object,
        required: true,
    },
})
const menuList = ref([])
const ruleFormRef = ref()
const defaultProps = {
    children: 'children', // 指定子树为节点对象的某个属性值
    label: 'menu_name', // 指定节点标签为节点对象的某个属性值
    value: 'id', // 指定节点值为节点对象的某个属性值
}
const terrNode = ref(props.menuInfo.parent_id)

getMenuLists({}).then((res) => {
    if (res?.code === 200) {
        menuList.value = [
            {
                id: 0,
                menu_name: '根菜单',
                disabled: false,
                children: res.data,
            },
        ]
    }
})

const currentChange = (node) => {
    props.menuInfo.parent_id = node.id
}

const changeState = (state) => {
    if (state !== '1') {
        props.menuInfo.component_path = ''
        props.menuInfo.component_name = ''
    }
}

defineExpose({
    ruleFormRef,
})
</script>
