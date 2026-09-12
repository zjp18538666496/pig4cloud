<template>
    <el-form ref="ruleFormRef" :model="props.roleInfo" :rules="rules" label-width="auto" style="max-width: 600px">
        <el-form-item label="昵称">
            <el-input v-model="props.roleInfo.name" />
        </el-form-item>
        <el-form-item label="用户名">
            <el-input disabled v-model="props.roleInfo.username" />
        </el-form-item>
        <el-form-item label="所属部门">
            <el-tree-select
                v-model="props.roleInfo.dept_id"
                :data="deptTree"
                :props="treeProps"
                check-strictly
                :render-after-expand="false"
                clearable
                placeholder="请选择部门（可选）"
                style="width: 100%"
            />
        </el-form-item>
        <el-form-item v-if="isSuper" label="所属租户">
            <el-select
                v-model="props.roleInfo.tenant_id"
                :disabled="props.roleInfo.id != null"
                clearable
                placeholder="不选则建到平台层(0)"
                style="width: 100%"
            >
                <el-option label="平台层(0)" :value="0" />
                <el-option v-for="item in tenantList" :key="item.id" :label="`${item.tenant_name}(${item.id})`" :value="item.id" />
            </el-select>
        </el-form-item>
        <el-form-item label="岗位">
            <el-select v-model="props.roleInfo.post_ids" multiple collapse-tags placeholder="请选择岗位（可选）" clearable style="width: 100%">
                <el-option v-for="item in postList" :key="item.id" :label="item.post_name" :value="item.id" />
            </el-select>
        </el-form-item>
        <el-form-item label="IP白名单">
            <el-input v-model="props.roleInfo.login_ip_whitelist"
                placeholder="登录IP白名单，逗号分隔，支持*通配；留空不限制" maxlength="500" />
        </el-form-item>
        <el-form-item label="手机号">
            <el-input v-model="props.roleInfo.mobile" />
        </el-form-item>
        <el-form-item label="邮箱">
            <el-input v-model="props.roleInfo.email" />
        </el-form-item>
        <el-form-item label="角色">
            <el-select v-model="props.roleInfo.role_codes" multiple placeholder="请选择角色" @change="(values) => (props.roleInfo.role_codes = values)" @clear="() => (props.roleInfo.role_codes = [])">
                <el-option v-for="item in roleLists" :key="item.role_code" :label="item.role_name" :value="item.role_code" />
            </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
            <el-date-picker v-model="props.roleInfo.create_time" type="datetime" placeholder="创建时间" disabled />
        </el-form-item>
        <el-form-item label="最后登录时间">
            <el-date-picker v-model="props.roleInfo.last_login_time" type="datetime" placeholder="最后登录时间" disabled />
        </el-form-item>
    </el-form>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getRoleLists } from '@/api/role.js'
import { getDeptTree } from '@/api/dept.js'
import { getTenantLists } from '@/api/tenant.js'
import { getEnabledPosts } from '@/api/post.js'

const roleLists = ref([])
const deptTree = ref([])
const postList = ref([])
const treeProps = {
    children: 'children',
    label: 'dept_name',
    value: 'id',
}
const ruleFormRef = ref()
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
getRoleLists({}).then((res) => {
    if (res.code === 200) {
        roleLists.value = res.data
    }
})
getEnabledPosts().then((res) => {
    if (res?.code === 200) {
        postList.value = res.data
    }
})

// 部门树跟随所选租户：超管新增用户选定租户后，部门选项切换为该租户的部门
const loadDeptTree = () => {
    getDeptTree({ tenant_id: props.roleInfo.tenant_id ?? null }).then((res) => {
        if (res?.code === 200) {
            deptTree.value = res.data
        }
    })
}
loadDeptTree()
watch(
    () => props.roleInfo.tenant_id,
    () => {
        props.roleInfo.dept_id = null
        loadDeptTree()
    },
)

// 所属租户仅平台超管可见/可指定（新增时）；租户归属建后不可改
const isSuper = (() => {
    try {
        return (JSON.parse(localStorage.getItem('userinfo'))?.permissions || []).includes('super')
    } catch {
        return false
    }
})()
const tenantList = ref([])
if (isSuper) {
    getTenantLists({ page: 1, pageSize: 100 }).then((res) => {
        if (res?.code === 200) {
            tenantList.value = res.data.rows
        }
    })
}
defineExpose({
    ruleFormRef,
})
</script>
