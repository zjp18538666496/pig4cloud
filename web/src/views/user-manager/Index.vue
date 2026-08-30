<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EditUser from '@/views/user-manager/components/EditUser.vue'
import { debounce } from '@/utils/utils.js'
import { createUser, delUser, downloadImportTemplate, exportUsers, getUserLists, importUsers, updateUser } from '@/api/user.js'
import { getDeptTree } from '@/api/dept.js'
import { getTenantLists } from '@/api/tenant.js'
import BaseTable from '@/utils/table.js'
import { VerifyUser } from '@/utils/vali.js'

const verifyUser = new VerifyUser()

const baseTable = new BaseTable()
baseTable.table.query.username = ''
baseTable.table.query.dept_id = null
baseTable.table.query.tenant_id = null
let userTable = ref(baseTable.table)
let roleRef = ref()

// 所属租户筛选/页签仅平台超管可见
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
const deptTree = ref([])
const loadDeptTree = () => {
    // 部门树跟随租户筛选：超管切换租户后部门选项也切换
    getDeptTree({ tenant_id: userTable.value.query.tenant_id }).then((res) => {
        if (res?.code === 200) {
            deptTree.value = res.data
        }
    })
}
loadDeptTree()

let user = ref({
    roleInfo: null,
    dialogVisible: false,
})

const initRoleInfo = () => {
    user.value.roleInfo = {}
}

let type = null

const rules = reactive({
    roleName: [{ validator: verifyUser.username, trigger: 'blur' }],
    roleCode: [{ validator: verifyUser.password, trigger: 'blur' }],
})
/**
 * 获取用户列表
 */
const getUserLise = () => {
    getUserLists(userTable.value.query).then((res) => {
        if (res?.code === 200) {
            userTable.value.rows = res.data.rows
            userTable.value.pagination.total = res.data.total
            userTable.value.query.pageSize = res.data.size
        }
    })
}

getUserLise()

/**
 * 查询/重置/切换租户页签：均回到第一页
 */
const handleSearch = () => {
    userTable.value.query.page = 1
    getUserLise()
}

const handleReset = () => {
    userTable.value.query.username = ''
    userTable.value.query.dept_id = null
    userTable.value.query.tenant_id = null
    handleSearch()
}

const handleTenantChange = () => {
    userTable.value.query.dept_id = null
    loadDeptTree()
    userTable.value.query.page = 1
    getUserLise()
}

/**
 * blob下载工具
 */
const downloadBlob = (blob, filename) => {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
}

const handleExport = () => {
    exportUsers(userTable.value.query).then((res) => {
        if (res instanceof Blob) {
            downloadBlob(res, '用户列表.xlsx')
        } else {
            ElMessage.error('导出失败，请重试')
        }
    })
}

const handleTemplate = () => {
    downloadImportTemplate().then((res) => {
        if (res instanceof Blob) {
            downloadBlob(res, '用户导入模板.xlsx')
        } else {
            ElMessage.error('模板下载失败，请重试')
        }
    })
}

const importInputRef = ref()
const handleImportClick = () => {
    importInputRef.value.click()
}
const handleImportFile = (event) => {
    const file = event.target.files[0]
    event.target.value = ''
    if (!file) return
    importUsers(file).then((res) => {
        if (res?.code === 200) {
            const { successCount, failures } = res.data
            if (!failures.length) {
                ElMessage.success(`导入成功：${successCount}名用户`)
            } else {
                ElMessageBox.alert(
                    `成功 ${successCount} 人，失败 ${failures.length} 人：` +
                        failures.map((f) => `第${f.row}行(${f.username})：${f.reason}`).join('；'),
                    '导入结果',
                    { type: 'warning' },
                )
            }
            getUserLise()
        } else {
            ElMessage.error(`导入失败！${res?.message}`)
        }
    })
}

/**
 * 更新表格高度
 */
const updateTableHeight = () => {
    userTable.value.height = window.innerHeight - 50 - 30 - 40 - 52 - 52 // 根据实际情况调整
}

/*
 * 弹窗关闭
 */
const handleClose = () => {
    roleRef.value.ruleFormRef.resetFields()
    setTimeout(() => (user.value.dialogVisible = false))
    initRoleInfo()
}

const createRole1 = () => {
    type = 'create'
    initRoleInfo()
    user.value.dialogVisible = true
}

/**
 * 删除
 * @param {number} index
 * @param {Object} row
 */
const handleDelete = (index, row) => {
    ElMessageBox.confirm(`您确定删除【${row.name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    })
        .then(() => delUser(row))
        .then((res) => {
            if (res?.code === 200) {
                ElMessage({ message: '删除成功', type: 'success' })
                getUserLise()
            } else {
                ElMessage.error(`删除失败！${res.message}`)
            }
        })
        .catch((error) => {
            ElMessage({ message: error.message || error, type: 'error' })
        })
}

/**
 * 编辑
 * @param index
 * @param row
 */
const handleEdit = (index, row) => {
    type = 'edit'
    user.value.roleInfo = { ...row }
    user.value.roleInfo.role_codes = row.role_codes?.split(',').filter((item) => item !== '')
    user.value.roleInfo.post_ids = row.post_ids ? row.post_ids.split(',').filter((item) => item !== '').map(Number) : []
    user.value.dialogVisible = true
}

/**
 * 保存用户
 */
const saveRole = () => {
    roleRef.value.ruleFormRef.validate((valid) => {
        if (!valid) return

        const handleResponse = (res) => {
            if (res?.code === 200) {
                user.value.dialogVisible = false
                ElMessage({ message: '保存成功', type: 'success' })
                initRoleInfo()
                getUserLise()
            } else {
                ElMessage.error(`保存失败！${res.message}`)
            }
        }

        switch (type) {
            case 'create':
                createUser(user.value.roleInfo).then(handleResponse)
                break
            case 'edit':
                updateUser(user.value.roleInfo).then(handleResponse)
                break
            default:
                break
        }
    })
}

/**
 * 监听窗口大小改变，重新计算表格高度
 */
onMounted(() => {
    // 监听窗口大小改变，重新计算表格高度
    const updateTableHeightFunc = debounce(updateTableHeight, 500)
    window.addEventListener('resize', updateTableHeightFunc)
})

/**
 * 监听窗口大小改变，重新计算表格高度
 */
onUnmounted(() => {
    window.removeEventListener('resize', updateTableHeight)
})
</script>

<template>
    <div>
        <div class="mb-20px">
            <template v-if="isSuper">
                所属租户
                <el-select v-model="userTable.query.tenant_id" class="w-200px" placeholder="全部租户" clearable @change="handleTenantChange">
                    <el-option label="平台层(0)" :value="0" />
                    <el-option v-for="item in tenantList" :key="item.id" :label="`${item.tenant_name}(${item.id})`" :value="item.id" />
                </el-select>
            </template>
            <el-input v-model="userTable.query.username" class="w-200px ml-10px" placeholder="用户名/姓名" clearable @keyup.enter="handleSearch" />
            <el-tree-select
                v-model="userTable.query.dept_id"
                :data="deptTree"
                :props="{ children: 'children', label: 'dept_name', value: 'id' }"
                check-strictly
                clearable
                placeholder="所属部门(含子部门)"
                style="width: 200px"
                class="ml-10px"
            />
            <el-button class="ml-10px" type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button v-permission="['user:write']" type="primary" @click="createRole1">新增</el-button>
            <el-button v-permission="['user:write']" @click="handleExport">导出</el-button>
            <el-button v-permission="['user:write']" @click="handleTemplate">导入模板</el-button>
            <el-button v-permission="['user:write']" @click="handleImportClick">导入</el-button>
            <input ref="importInputRef" type="file" accept=".xlsx,.xls" style="display: none" @change="handleImportFile" />
        </div>
        <el-table :data="userTable.rows" border class="w-100% overflow-auto mb-10px" :max-height="userTable.height">
            <el-table-column prop="date" label="序号" align="center" width="60">
                <template #default="scope">
                    <span>{{ scope.$index + 1 }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="name" label="昵称" align="center" />
            <el-table-column prop="username" label="用户名称" align="center" />
            <el-table-column prop="tenant_name" label="所属租户" align="center" width="120">
                <template #default="scope">
                    {{ scope.row.tenant_name || `平台(${scope.row.tenant_id})` }}
                </template>
            </el-table-column>
            <el-table-column prop="dept_name" label="所属部门" align="center">
                <template #default="scope">
                    {{ scope.row.dept_name || '-' }}
                </template>
            </el-table-column>
            <el-table-column prop="post_names" label="岗位" align="center">
                <template #default="scope">
                    <div class="flex flex-wrap gap-6px justify-center">
                        <el-tag v-for="(item, key) in (scope.row.post_names || '').split(',').filter((item) => item !== '')" :key="key" type="warning" size="small">
                            {{ item }}
                        </el-tag>
                        <span v-if="!scope.row.post_names">-</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="mobile" label="手机号" align="center" />
            <el-table-column prop="email" label="邮箱" align="center" />
            <el-table-column prop="role_names" label="角色" align="center">
                <template #default="scope">
                    <div class="flex flex-wrap gap-10px">
                        <el-tag type="primary" v-if="scope.row.role_names" v-for="(item, key) in scope.row.role_names.split(',')" :key="key">
                            {{ item }}
                        </el-tag>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="create_time" label="创建时间" align="center" />
            <el-table-column prop="update_time" label="更新时间" align="center" />
            <el-table-column prop="last_login_time" label="最后登录时间" align="center" />
            <el-table-column prop="address" label="操作" align="center">
                <template #default="scope">
                    <el-button v-permission="['user:write']" size="small" @click="handleEdit(scope.$index, scope.row)"> 编辑</el-button>
                    <el-button v-permission="['user:remove']" size="small" type="danger" @click="handleDelete(scope.$index, scope.row)"> 删除 </el-button>
                </template>
            </el-table-column>
        </el-table>
        <div class="flex justify-end w-100%">
            <el-pagination
                class="mb-20px"
                v-model:current-page="userTable.query.page"
                v-model:page-size="userTable.query.pageSize"
                :page-sizes="userTable.pagination.pageSize"
                :disabled="userTable.pagination.disabled"
                :background="userTable.pagination.background"
                :layout="userTable.pagination.layout"
                :total="userTable.pagination.total"
                @size-change="getUserLise"
                @current-change="getUserLise"
            />
        </div>
        <div>
            <el-dialog v-model="user.dialogVisible" title="编辑用户" width="800" :before-close="handleClose">
                <EditUser :rules="rules" :roleInfo="user.roleInfo" ref="roleRef" />
                <template #footer>
                    <div class="dialog-footer">
                        <el-button @click="user.dialogVisible = false">取消</el-button>
                        <el-button type="primary" @click="saveRole"> 保存</el-button>
                    </div>
                </template>
            </el-dialog>
        </div>
    </div>
</template>

<style scoped></style>
