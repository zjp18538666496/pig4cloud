<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { delRole, getRoleLists, updateRole, createRole } from '@/api/role.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import EditRole from '@/views/role-manager/components/EditRole.vue'
import { debounce } from '@/utils/utils.js'

let roleTable = ref({
    // 表格数据
    rows: [],
    // 表格高度
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
    // 查询条件
    query: {
        roleName: '',
        page: 1,
        pageSize: 10,
    },
    // 表格分页组件属性
    pagination: {
        total: 0,
        disabled: false,
        background: false,
    },
})

let roleRef = ref()

let role = ref({
    roleInfo: null,
    dialogVisible: false,
})

const initRoleInfo = () => {
    role.value.roleInfo = {
        role_name: '',
        role_code: '',
        description: '',
    }
}

let type = null
/**
 * 表单验证
 */
const verifyUsername = (rule, value, callback) => {
    if (value) {
        callback()
    } else {
        callback(new Error('角色名称不能为空'))
    }
}
const verifyPassword = (rule, value, callback) => {
    if (value) {
        callback()
    } else {
        callback(new Error('角色编码不能为空'))
    }
}
const rules = reactive({
    role_name: [{ validator: verifyUsername, trigger: 'blur' }],
    role_code: [{ validator: verifyPassword, trigger: 'blur' }],
})
/**
 * 获取角色列表
 */
const getRoleList = () => {
    getRoleLists(roleTable.value.query).then((res) => {
        if (res?.code === 200) {
            roleTable.value.rows = res.data.rows
            roleTable.value.pagination.total = res.data.total
            roleTable.value.query.pageSize = res.data.size
        }
    })
}

getRoleList()

/**
 * 更新表格高度
 */
const updateTableHeight = () => {
    roleTable.value.height = window.innerHeight - 50 - 30 - 40 - 52 - 52 // 根据实际情况调整
}

/*
 * 弹窗关闭
 */
const handleClose = () => {
    roleRef.value.ruleFormRef.resetFields()
    setTimeout(() => (role.value.dialogVisible = false))
    initRoleInfo()
}

const createRole1 = () => {
    type = 'create'
    initRoleInfo()
    role.value.dialogVisible = true
}

/**
 * 删除
 * @param index
 * @param row
 */
const handleDelete = (index, row) => {
    ElMessageBox.confirm(`您确定删除【${row.role_name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    })
        .then(() => {
            delRole({
                role_code: row.role_code,
            }).then((res) => {
                if (res?.code === 200) {
                    ElMessage({ message: '删除成功', type: 'success' })
                    getRoleList()
                } else {
                    ElMessage.error(`删除失败！${res.message}`)
                }
            })
        })
        .catch(() => {})
}

/**
 * 编辑
 * @param index
 * @param row
 */
const handleEdit = (index, row) => {
    type = 'edit'
    role.value.roleInfo = { ...row }
    role.value.roleInfo.menu_codes = role.value.roleInfo.menu_codes === '' ? [] : role.value.roleInfo.menu_codes.split(',').map((item) => Number(item.trim()))
    role.value.dialogVisible = true
}

/**
 * 保存角色
 */
const saveRole = () => {
    role.value.roleInfo.menu_codes = Array.from(new Set([...role.value.roleInfo.menu_codes, ...roleRef.value.HalfCheckedKeys]))
    role.value.roleInfo.menu_codes = role.value.roleInfo.menu_codes?.join(',')
    roleRef.value.ruleFormRef.validate((valid) => {
        if (valid) {
            switch (type) {
                case 'create':
                    createRole(role.value.roleInfo).then((res) => {
                        if (res?.code === 200) {
                            role.value.dialogVisible = false
                            initRoleInfo()
                            ElMessage({ message: '保存成功', type: 'success' })
                            getRoleList()
                        } else {
                            ElMessage.error(`保存失败！${res.message}`)
                        }
                    })
                    break
                case 'edit':
                    updateRole(role.value.roleInfo).then((res) => {
                        if (res?.code === 200) {
                            role.value.dialogVisible = false
                            initRoleInfo()
                            ElMessage({ message: '保存成功', type: 'success' })
                            getRoleList()
                        } else {
                            ElMessage.error(`保存失败！${res.message}`)
                        }
                    })
                    break
                default:
                    break
            }
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
        <div style="margin-bottom: 20px">
            角色名称
            <el-input v-model="roleTable.query.roleName" style="width: 240px" placeholder="角色名称" />
            <el-button style="margin: 0 0 0 10px" @click="getRoleList">查询</el-button>
            <el-button type="primary" @click="roleTable.query.roleName = ''">重置</el-button>
            <el-button type="primary" @click="createRole1">新增</el-button>
        </div>
        <el-table :data="roleTable.rows" border style="width: 100%; overflow: auto" :max-height="roleTable.height">
            <el-table-column prop="date" label="序号" align="center" width="60">
                <template #default="scope">
                    <span>{{ scope.$index + 1 }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="role_name" label="角色名称" align="center" />
            <el-table-column prop="role_code" label="角色标识" align="center">
                <template #default="scope">
                    <el-tag type="primary">{{ scope.row.role_code }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="description" label="角色描述" align="center" />
            <el-table-column prop="address" label="菜单权限" align="center">
                <template #default="scope">
                    <div style="display: flex; flex-wrap: wrap; gap: 10px">
                        <el-tag v-for="(itme, key) in scope.row.menu_names?.split(',')" type="primary">{{ itme }}</el-tag>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="address" label="创建时间" align="center" />
            <el-table-column prop="address" label="操作" align="center">
                <template #default="scope">
                    <el-button size="small" @click="handleEdit(scope.$index, scope.row)"> 编辑</el-button>
                    <el-button size="small" type="danger" @click="handleDelete(scope.$index, scope.row)"> 删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; width: 100%">
            <el-pagination
                v-model:current-page="roleTable.query.page"
                v-model:page-size="roleTable.query.pageSize"
                :page-sizes="[10, 20, 30, 40]"
                style="margin-top: 20px"
                :disabled="roleTable.pagination.disabled"
                :background="roleTable.pagination.background"
                layout="total, sizes, prev, pager, next, jumper"
                :total="roleTable.pagination.total"
                @size-change="getRoleList"
                @current-change="getRoleList"
            />
        </div>
        <div>
            <el-dialog v-model="role.dialogVisible" title="编辑角色" width="800" :before-close="handleClose">
                <EditRole v-if="role.dialogVisible" :rules="rules" :roleInfo="role.roleInfo" ref="roleRef" />
                <template #footer>
                    <div class="dialog-footer">
                        <el-button @click="role.dialogVisible = false">取消</el-button>
                        <el-button type="primary" @click="saveRole"> 保存</el-button>
                    </div>
                </template>
            </el-dialog>
        </div>
    </div>
</template>

<style scoped></style>
