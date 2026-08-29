<template>
    <div>
        <div class="mb-20px">
            租户名称
            <el-input v-model="tenantTable.query.tenant_name" class="w240px" placeholder="租户名称" @keyup.enter="handleSearch" />
            <el-button class="ml-10px" @click="handleSearch">查询</el-button>
            <el-button type="primary" @click="handleReset">重置</el-button>
            <el-button v-permission="['tenant:manage']" type="primary" @click="openCreate">开通租户</el-button>
        </div>
        <el-table :data="tenantTable.rows" border style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="tenant_code" label="租户编码" width="160" align="center" />
            <el-table-column prop="tenant_name" label="租户名称" align="left" />
            <el-table-column prop="status" label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">
                        {{ scope.row.status === '1' ? '启用' : '禁用' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="create_time" label="创建时间" width="170" align="center" />
            <el-table-column label="操作" width="140" align="center">
                <template #default="scope">
                    <el-button v-permission="['tenant:manage']" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            v-model:current-page="tenantTable.query.page"
            v-model:page-size="tenantTable.query.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="tenantTable.total"
            layout="total, sizes, prev, pager, next, jumper"
            class="mt-10px justify-end"
            @current-change="getTenantList"
            @size-change="handleSizeChange"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '开通租户' : '编辑租户'" width="480px">
            <el-form ref="formRef" :model="dialog.form" label-width="90px">
                <el-form-item label="租户编码" :rules="dialog.type === 'create' ? rules.tenant_code : []" prop="tenant_code">
                    <el-input v-model="dialog.form.tenant_code" :disabled="dialog.type === 'edit'" placeholder="如companyA" />
                </el-form-item>
                <el-form-item label="租户名称" prop="tenant_name" :rules="rules.tenant_name">
                    <el-input v-model="dialog.form.tenant_name" placeholder="租户名称" />
                </el-form-item>
                <el-form-item v-if="dialog.type === 'edit'" label="状态">
                    <el-radio-group v-model="dialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-alert
                    v-if="dialog.type === 'create'"
                    type="info"
                    :closable="false"
                    title="开通后自动创建管理员账号：{编码}_admin，初始密码 12345678，并授予全部菜单权限"
                />
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" @click="submit">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createTenant, getTenantLists, updateTenant } from '@/api/tenant.js'

const tenantTable = reactive({
    query: { tenant_name: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
})

const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, tenant_code: '', tenant_name: '', status: '1' },
})

const formRef = ref()
const rules = {
    tenant_code: [{ required: true, message: '租户编码不能为空', trigger: 'blur' }],
    tenant_name: [{ required: true, message: '租户名称不能为空', trigger: 'blur' }],
}

const getTenantList = () => {
    getTenantLists(tenantTable.query).then((res) => {
        if (res?.code === 200) {
            tenantTable.rows = res.data.rows
            tenantTable.total = res.data.total
        }
    })
}

const handleSearch = () => {
    tenantTable.query.page = 1
    getTenantList()
}

const handleReset = () => {
    tenantTable.query.tenant_name = ''
    handleSearch()
}

const handleSizeChange = () => {
    tenantTable.query.page = 1
    getTenantList()
}

const openCreate = () => {
    dialog.type = 'create'
    dialog.form = { id: null, tenant_code: '', tenant_name: '', status: '1' }
    dialog.visible = true
}

const handleEdit = (row) => {
    dialog.type = 'edit'
    dialog.form = { id: row.id, tenant_code: row.tenant_code, tenant_name: row.tenant_name, status: row.status }
    dialog.visible = true
}

const submit = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        const action = dialog.type === 'create' ? createTenant : updateTenant
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                ElMessage({ message: res.message || '保存成功', type: 'success' })
                dialog.visible = false
                getTenantList()
            } else {
                ElMessage.error(`保存失败！${res.message}`)
            }
        })
    })
}

onMounted(getTenantList)
</script>
