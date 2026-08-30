<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDept, delDept, getDeptTree, updateDept } from '@/api/dept.js'
import { getTenantLists } from '@/api/tenant.js'

/**
 * 部门管理：树形表格，一次加载全树；
 * 超管可按租户筛选查看（部门本身按租户隔离，普通管理员只见本租户）
 */
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

const tenantQuery = reactive({ tenant_id: null })

const deptTable = reactive({
    rows: [],
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const loadTree = () => {
    getDeptTree({ tenant_id: tenantQuery.tenant_id }).then((res) => {
        if (res?.code === 200) {
            deptTable.rows = res.data
        } else {
            ElMessage.error(`获取部门失败！${res?.message}`)
        }
    })
}
loadTree()

const handleTenantChange = () => {
    loadTree()
}

const tenantLabel = (tenantId) => {
    const item = tenantList.value.find((t) => t.id === tenantId)
    return item ? item.tenant_name : `平台层(${tenantId})`
}

/**
 * 新增/编辑弹窗
 */
const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, parent_id: 0, dept_name: '', sort: 0, tenant_id: null },
})
const formRef = ref()
// 上级部门选择器数据源（含根节点）
const parentOptions = ref([])

const openDialog = (type, row) => {
    dialog.type = type
    if (type === 'create') {
        // 新增部门归属当前筛选的租户（超管未筛选租户时为空=平台层）
        dialog.form = { id: null, parent_id: 0, dept_name: '', sort: 0, tenant_id: tenantQuery.tenant_id }
    } else {
        // 编辑时租户归属不可改
        dialog.form = { id: row.id, parent_id: row.parent_id, dept_name: row.dept_name, sort: row.sort, tenant_id: row.tenant_id }
    }
    // 树形选择父部门：数据源即当前筛选租户的部门树，天然同租户
    parentOptions.value = [{ id: 0, dept_name: '根部门', children: deptTable.rows }]
    dialog.visible = true
}

const rules = reactive({
    dept_name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
})

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) {
            return
        }
        const action = dialog.type === 'create' ? createDept : updateDept
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '保存成功')
                dialog.visible = false
                loadTree()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`删除部门【${row.dept_name}】时，会删除其下所有子部门，您确定删除吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        delDept({ id: row.id }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '删除成功')
                loadTree()
            } else {
                ElMessage.error(`删除失败！${res?.message}`)
            }
        })
    })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between mb-10px">
            <div class="flex items-center gap-10px">
                <template v-if="isSuper">
                    所属租户
                    <el-select v-model="tenantQuery.tenant_id" class="w-200px" placeholder="全部租户" clearable @change="handleTenantChange">
                        <el-option label="平台层(0)" :value="0" />
                        <el-option v-for="item in tenantList" :key="item.id" :label="`${item.tenant_name}(${item.id})`" :value="item.id" />
                    </el-select>
                </template>
            </div>
            <el-button type="primary" v-permission="['dept:write']" @click="openDialog('create')">新增部门</el-button>
        </div>
        <el-table :data="deptTable.rows" border row-key="id" :max-height="deptTable.height" :tree-props="{ children: 'children' }" default-expand-all>
            <el-table-column prop="dept_name" label="部门名称" min-width="240" />
            <el-table-column prop="sort" label="排序" width="90" align="center" />
            <el-table-column label="所属租户" width="140" align="center">
                <template #default="scope">
                    {{ tenantLabel(scope.row.tenant_id) }}
                </template>
            </el-table-column>
            <el-table-column prop="create_time" label="创建时间" width="180" />
            <el-table-column label="操作" width="160" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link v-permission="['dept:write']" @click="openDialog('edit', scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link v-permission="['dept:remove']" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '新增部门' : '编辑部门'" width="480" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
                <el-form-item v-if="isSuper" label="所属租户">
                    <el-select v-model="dialog.form.tenant_id" :disabled="dialog.type === 'edit'" clearable placeholder="不选为平台层(0)" style="width: 100%">
                        <el-option label="平台层(0)" :value="0" />
                        <el-option v-for="item in tenantList" :key="item.id" :label="`${item.tenant_name}(${item.id})`" :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="上级部门">
                    <el-tree-select
                        v-model="dialog.form.parent_id"
                        :data="parentOptions"
                        :props="{ children: 'children', label: 'dept_name', value: 'id' }"
                        check-strictly
                        :render-after-expand="false"
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="部门名称" prop="dept_name">
                    <el-input v-model="dialog.form.dept_name" placeholder="请输入部门名称" maxlength="64" />
                </el-form-item>
                <el-form-item label="显示排序">
                    <el-input-number v-model="dialog.form.sort" :min="0" :max="999" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
