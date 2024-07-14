<template>
    <el-form ref="ruleFormRef" :model="props.roleInfo" :rules="rules" label-width="auto" style="max-width: 600px">
        <el-form-item prop="roleName" label="角色名称">
            <el-input v-model="props.roleInfo.role_name" />
        </el-form-item>
        <el-form-item prop="roleCode" label="角色编码">
            <el-input v-model="props.roleInfo.role_code" />
        </el-form-item>
        <el-form-item prop="description" label="角色描述">
            <el-input v-model="props.roleInfo.description" />
        </el-form-item>
        <el-form-item prop="description" label="菜单权限">
            <el-tree-select
                v-model="terrNode"
                :default-checked-keys="terrNode"
                :data="menuList"
                :render-after-expand="false"
                :props="defaultProps"
                node-key="id"
                multiple
                collapse-tags
                show-checkbox
                :check-strictly="false"
                ref="tree"
                @check-change="getCheckedNodes"
            ></el-tree-select>
        </el-form-item>
    </el-form>
</template>

<script setup>
import { ref } from 'vue'
import { getMenuLists } from '@/api/menu.js'

const props = defineProps({
    roleInfo: {
        type: Object,
        required: true,
    },
    rules: {
        type: Object,
        required: true,
    },
})
const terrNode = ref(props.roleInfo.menu_codes)
const HalfCheckedKeys = ref([])
const tree = ref()
const menuList = ref([])
const ruleFormRef = ref()
const defaultProps = {
    children: 'children', // 指定子树为节点对象的某个属性值
    label: 'menu_name', // 指定节点标签为节点对象的某个属性值
    value: 'id', // 指定节点值为节点对象的某个属性值
    disabled: 'xxx',
}
getMenuLists({}).then((res) => {
    if (res?.code === 200) {
        menuList.value = res.data
    }
})

const getCheckedNodes = () => {
    // 获取所有选中节点的数据
    HalfCheckedKeys.value = tree.value.getHalfCheckedKeys()
    terrNode.value = []
    const checkedNodes = tree.value.getCheckedNodes()
    checkedNodes.forEach(function (item) {
        terrNode.value.push(item.id)
    })
    props.roleInfo.menu_codes = terrNode.value
    console.log('选中的节点数据:', checkedNodes)
}
defineExpose({
    ruleFormRef,
    HalfCheckedKeys,
})
</script>
