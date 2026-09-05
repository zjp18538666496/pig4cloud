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
        <el-form-item prop="data_scope" label="数据权限">
            <el-radio-group v-model="props.roleInfo.data_scope">
                <el-radio value="1">本租户全部</el-radio>
                <el-radio value="2">本部门及以下</el-radio>
                <el-radio value="3">仅本人</el-radio>
                <el-radio value="4">自定义部门集</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item v-if="props.roleInfo.data_scope === '4'" label="部门范围">
            <el-tree-select v-model="customDeptIdList" :data="deptTreeOptions" multiple
                :props="{ label: 'dept_name', children: 'children' }" node-key="id" check-strictly
                show-checkbox :render-after-expand="false" style="width: 100%" placeholder="勾选可见的部门（精确到勾选项）" />
        </el-form-item>
        <el-form-item prop="parent_id" label="上级角色">
            <el-tree-select
                v-model="props.roleInfo.parent_id"
                :data="parentOptions"
                :props="roleTreeProps"
                check-strictly
                :render-after-expand="false"
                style="width: 100%"
            />
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
        <el-alert type="info" :closable="false" title="子角色自动继承上级角色的菜单权限（自身勾选的菜单叠加）；角色编码与数据权限不继承" />
    </el-form>
</template>

<script setup>
import { computed, ref } from 'vue'
import { getMenuLists } from '@/api/menu.js'
import { getRoleLists } from '@/api/role.js'
import { getDeptTree } from '@/api/dept.js'

// 自定义部门集：custom_dept_ids(逗号分隔字符串) <-> 多选树id数组
const deptTreeOptions = ref([])
getDeptTree({}).then((res) => {
    if (res?.code === 200) deptTreeOptions.value = res.data
})
const customDeptIdList = computed({
    get: () => (props.roleInfo.custom_dept_ids || '').split(',').filter(Boolean).map(Number),
    set: (ids) => { props.roleInfo.custom_dept_ids = (ids || []).join(',') },
})

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

/**
 * 上级角色选择：同租户角色树，排除自身/自身子孙(防成环)与super及其子树(防越权继承)
 */
const roleTreeProps = {
    children: 'children',
    label: 'role_name',
    value: 'id',
}
const parentOptions = ref([])
getRoleLists({}).then((res) => {
    if (res?.code !== 200 || !Array.isArray(res.data)) {
        return
    }
    const selfId = props.roleInfo.id
    const sameTenant = (props.roleInfo.tenant_id ?? 0)
    const roles = (res.data || []).filter(
        (role) => (role.tenant_id ?? 0) === sameTenant && role.role_code !== 'super' && role.id !== selfId,
    )
    // 排除自身子孙与super子孙：先按parent挂索引，剔除被排除节点的整棵子树
    const excluded = new Set()
    if (selfId != null) {
        excluded.add(selfId)
    }
    roles.forEach((role) => {
        if (role.role_code === 'super') {
            excluded.add(role.id)
        }
    })
    let changed = true
    while (changed) {
        changed = false
        roles.forEach((role) => {
            // 父角色被排除(自身/super链)则整棵子树都不可作为上级候选
            if (!excluded.has(role.id) && role.parent_id && excluded.has(role.parent_id)) {
                excluded.add(role.id)
                changed = true
            }
        })
    }
    const validIds = new Set(roles.filter((role) => !excluded.has(role.id)).map((role) => role.id))
    const childrenMap = new Map()
    roles.forEach((role) => {
        if (excluded.has(role.id)) {
            return
        }
        const pid = role.parent_id && validIds.has(role.parent_id) ? role.parent_id : 0
        if (!childrenMap.has(pid)) {
            childrenMap.set(pid, [])
        }
        childrenMap.get(pid).push({ ...role, children: [] })
    })
    const attach = (node) => {
        node.children = (childrenMap.get(node.id) || []).map(attach)
        return node
    }
    parentOptions.value = [{ id: 0, role_name: '顶级角色(无上级)', children: (childrenMap.get(0) || []).map(attach) }]
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
}
defineExpose({
    ruleFormRef,
    HalfCheckedKeys,
})
</script>
