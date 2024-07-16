<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EditMenu from '@/views/menu-manager/components/EditMenu.vue'
import { debounce } from '@/utils/utils.js'
import { createMenu, delMenu, getMenuLists, updateMenu } from '@/api/menu.js'
import emitter from '@/utils/mitt.js'
import BaseTable from '@/utils/table.js'

const baseTable = new BaseTable()
baseTable.table.query.menu_name = ''
let menuTable = ref(baseTable.table)
let menuRef = ref()
let menu = ref({
    menuInfo: null,
    dialogVisible: false,
})

const initMenuInfo = () => {
    menu.value.menuInfo = {}
}

let type = null
/**
 * 表单验证
 */
const verifyUsername = (rule, value, callback) => {
    if (value) {
        callback()
    } else {
        callback(new Error('用户名称不能为空'))
    }
}
const verifyPassword = (rule, value, callback) => {
    if (value) {
        callback()
    } else {
        callback(new Error('用户编码不能为空'))
    }
}
const rules = reactive({
    menuName: [{ validator: verifyUsername, trigger: 'blur' }],
    menuCode: [{ validator: verifyPassword, trigger: 'blur' }],
})
/**
 * 获取用户列表
 */
const getUserLise = () => {
    getMenuLists(menuTable.value.query).then((res) => {
        if (res?.code === 200) {
            menuTable.value.rows = res.data
        }
    })
}

getUserLise()

/**
 * 更新表格高度
 */
const updateTableHeight = () => {
    menuTable.value.height = window.innerHeight - 50 - 30 - 40 - 52 - 52 // 根据实际情况调整
}

/*
 * 弹窗关闭
 */
const handleClose = () => {
    menuRef.value.ruleFormRef.resetFields()
    setTimeout(() => (menu.value.dialogVisible = false))
    initMenuInfo()
}

const createMenu1 = () => {
    type = 'create'
    initMenuInfo()
    menu.value.dialogVisible = true
}

/**
 * 删除
 * @param index
 * @param row
 */
const handleDelete = (index, row) => {
    ElMessageBox.confirm(`删除当前菜单时，会删除当前菜单下的所有子菜单，您确定删除【${row.menu_name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    })
        .then(() => delMenu(row))
        .then((res) => {
            if (res?.code === 200) {
                ElMessage({ message: '删除成功', type: 'success' })
                getUserLise()
                emitter.emit('refreshMenu')
            } else {
                ElMessage.error(`删除失败！${res.message}`)
            }
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
    menu.value.menuInfo = { ...row }
    menu.value.dialogVisible = true
}

/**
 * 保存用户
 */
const saveMenu = () => {
    menuRef.value.ruleFormRef.validate((valid) => {
        if (!valid) return
        const handleResponse = (res) => {
            if (res?.code === 200) {
                menu.value.dialogVisible = false
                initMenuInfo()
                ElMessage({ message: '保存成功', type: 'success' })
                getUserLise()
                emitter.emit('refreshMenu')
            } else {
                ElMessage.error(`保存失败！${res.message}`)
            }
        }
        if (valid) {
            switch (type) {
                case 'create':
                    createMenu(menu.value.menuInfo).then(handleResponse)
                    break
                case 'edit':
                    updateMenu(menu.value.menuInfo).then(handleResponse)
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
        <div class="mb-20px">
            菜单名称
            <el-input v-model="menuTable.query.menu_name" class="w240px" placeholder="菜单名称" />
            <el-button class="ml-10px" @click="getUserLise">查询</el-button>
            <el-button type="primary" @click="menuTable.query.menu_name = ''">重置</el-button>
            <el-button type="primary" @click="createMenu1">新增</el-button>
        </div>
        <el-table :data="menuTable.rows" border style="width: 100%; overflow: auto" :max-height="menuTable.height" row-key="id">
            <el-table-column prop="menu_name" label="菜单名称" align="center" />
            <el-table-column prop="route" label="路由地址" align="left">
                <template #default="scope">
                    <el-tag type="primary">{{ scope.row.route }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '0' ? 'danger' : 'primary'">
                        {{
                            {
                                '0': '禁用',
                                '1': '启用',
                            }[scope.row.status] || ''
                        }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="type" label="菜单类型" align="center">
                <template #default="scope">
                    <el-tag type="primary">
                        {{
                            {
                                '0': '目录',
                                '1': '菜单',
                                '2': '按钮',
                            }[scope.row.type] || ''
                        }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="level" label="层级" align="center" />
            <el-table-column prop="address" label="操作" align="center">
                <template #default="scope">
                    <el-button size="small" @click="handleEdit(scope.$index, scope.row)"> 编辑</el-button>
                    <el-button size="small" type="danger" @click="handleDelete(scope.$index, scope.row)"> 删除 </el-button>
                </template>
            </el-table-column>
        </el-table>
        <div>
            <el-dialog v-model="menu.dialogVisible" title="编辑菜单" width="800" :before-close="handleClose">
                <EditMenu v-if="menu.dialogVisible" :rules="rules" :menuInfo="menu.menuInfo" ref="menuRef" />
                <template #footer>
                    <div class="dialog-footer">
                        <el-button @click="menu.dialogVisible = false">取消</el-button>
                        <el-button type="primary" @click="saveMenu"> 保存</el-button>
                    </div>
                </template>
            </el-dialog>
        </div>
    </div>
</template>

<style scoped></style>
