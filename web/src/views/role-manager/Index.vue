<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { delRole, getRoleLists, updateRole, createRole } from '@/api/role.js'
import { getTenantLists } from '@/api/tenant.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import EditRole from '@/views/role-manager/components/EditRole.vue'
import { debounce } from '@/utils/utils.js'

// 菜单权限列最多展示的标签数，超出的折叠成"+N"
const MENU_TAG_LIMIT = 2
const DS_TEXT = { 1: '本租户全部', 2: '本部门及以下', 3: '仅本人' }

// 所属租户分组仅平台超管可见（普通管理员只见本租户角色）
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
const tenantName = (tenantId) => {
    const item = tenantList.value.find((t) => t.id === tenantId)
    return item ? item.tenant_name : `平台层(${tenantId})`
}

const roleTable = reactive({
    rows: [],
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
    query: { roleName: '' },
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
        parent_id: 0,
    }
}

let type = null

const menuNameList = (row) => (row.menu_names || '').split(',').filter((item) => item !== '')
const rowKey = (row) => (row.isTenant ? `tenant-${row.tenant_id}` : `role-${row.id}`)

/**
 * 获取角色全量并组装树形数据：
 * 超管视角以租户为虚拟父节点分组，角色按parent_id嵌套；搜索时保留命中节点及其祖先链
 */
const allRoles = ref([])
const getRoleList = () => {
    getRoleLists({}).then((res) => {
        if (res?.code === 200) {
            allRoles.value = res.data || []
            buildTree()
        } else {
            ElMessage.error(`获取角色失败！${res?.message}`)
        }
    })
}

const buildTree = () => {
    const keyword = (roleTable.query.roleName || '').trim()
    const match = (row) =>
        !keyword || (row.role_name || '').includes(keyword) || (row.role_code || '').includes(keyword)

    const validIds = new Set(allRoles.value.map((row) => row.id))
    const childrenMap = new Map()
    allRoles.value.forEach((row) => {
        // 父角色已不存在(孤儿)的按顶级处理
        const pid = row.parent_id && validIds.has(row.parent_id) ? row.parent_id : 0
        if (!childrenMap.has(pid)) {
            childrenMap.set(pid, [])
        }
        childrenMap.get(pid).push({ ...row, children: [] })
    })
    const attach = (node) => {
        node.children = (childrenMap.get(node.id) || []).map(attach)
        return node
    }
    let forest = (childrenMap.get(0) || []).map(attach)

    const filterTree = (nodes) => {
        const out = []
        for (const node of nodes) {
            const children = filterTree(node.children || [])
            if (match(node) || children.length) {
                out.push({ ...node, children })
            }
        }
        return out
    }
    forest = filterTree(forest)

    if (isSuper) {
        const byTenant = new Map()
        forest.forEach((root) => {
            const tid = root.tenant_id ?? 0
            if (!byTenant.has(tid)) {
                byTenant.set(tid, [])
            }
            byTenant.get(tid).push(root)
        })
        roleTable.rows = Array.from(byTenant.entries())
            .map(([tenantId, roles]) => ({
                isTenant: true,
                tenant_id: tenantId,
                role_name: tenantName(tenantId),
                role_code: '',
                description: `${roles.length}个角色`,
                menu_names: '',
                children: roles,
            }))
            .filter((group) => group.children.length > 0)
    } else {
        roleTable.rows = forest
    }
}

const handleSearch = () => buildTree()

const handleReset = () => {
    roleTable.query.roleName = ''
    buildTree()
}

getRoleList()

/**
 * 更新表格高度
 */
const updateTableHeight = () => {
    roleTable.height = window.innerHeight - 50 - 30 - 40 - 52 - 52
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
 * @param {number} index
 * @param {Object} row
 */
const handleDelete = (index, row) => {
    ElMessageBox.confirm(`您确定删除【${row.role_name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    })
        .then(() => delRole({ role_code: row.role_code }))
        .then((res) => {
            if (res?.code === 200) {
                ElMessage({ message: '删除成功', type: 'success' })
                getRoleList()
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
    role.value.roleInfo = { ...row, parent_id: row.parent_id || 0 }
    role.value.roleInfo.menu_codes = role.value.roleInfo.menu_codes === '' ? [] : role.value.roleInfo.menu_codes.split(',').map((item) => Number(item.trim()))
    role.value.dialogVisible = true
}
/**
 * 保存角色
 */
const saveRole = () => {
    role.value.roleInfo.menu_codes = Array.from(new Set([...role.value.roleInfo.menu_codes, ...roleRef.value.HalfCheckedKeys]))

    const handleResponse = (res) => {
        if (res?.code === 200) {
            role.value.dialogVisible = false
            initRoleInfo()
            ElMessage({ message: '保存成功', type: 'success' })
            getRoleList()
        } else {
            ElMessage.error(`保存失败！${res.message}`)
        }
    }

    roleRef.value.ruleFormRef.validate((valid) => {
        if (!valid) return
        switch (type) {
            case 'create':
                createRole(role.value.roleInfo).then(handleResponse)
                break
            case 'edit':
                updateRole(role.value.roleInfo).then(handleResponse)
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
    const updateTableHeightFunc = debounce(updateTableHeight, 500)
    window.addEventListener('resize', updateTableHeightFunc)
})

onUnmounted(() => {
    window.removeEventListener('resize', updateTableHeight)
})
</script>

<template>
    <div>
        <div class="mb-20px">
            角色名称
            <el-input v-model="roleTable.query.roleName" class="w240px" placeholder="角色名称/标识" clearable @keyup.enter="handleSearch" />
            <el-button class="ml-10px" type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button v-permission="['role:write']" type="primary" @click="createRole1">新增</el-button>
        </div>
        <el-table
            :data="roleTable.rows"
            border
            row-key="id"
            :row-key="rowKey"
            :tree-props="{ children: 'children' }"
            default-expand-all
            class="w-100% overflow-auto mb-10px"
            :max-height="roleTable.height"
        >
            <el-table-column prop="role_name" label="角色名称" min-width="220" />
            <el-table-column prop="role_code" label="角色标识" align="center" width="140">
                <template #default="scope">
                    <el-tag v-if="scope.row.role_code" type="primary">{{ scope.row.role_code }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="description" label="角色描述" align="center" />
            <el-table-column label="数据权限" align="center" width="120">
                <template #default="scope">
                    <el-tag v-if="scope.row.data_scope && !scope.row.isTenant" type="warning">{{ DS_TEXT[scope.row.data_scope] || scope.row.data_scope }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="address" label="菜单权限" align="center" min-width="260">
                <template #default="scope">
                    <div class="flex flex-wrap gap-10px justify-center">
                        <el-tag v-for="(item, index) in menuNameList(scope.row)" v-show="index < MENU_TAG_LIMIT" :key="item" type="primary">{{ item }}</el-tag>
                        <el-tooltip v-if="menuNameList(scope.row).length > MENU_TAG_LIMIT" placement="top" :content="menuNameList(scope.row).slice(MENU_TAG_LIMIT).join('、')">
                            <el-tag type="info">+{{ menuNameList(scope.row).length - MENU_TAG_LIMIT }}</el-tag>
                        </el-tooltip>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="address" label="操作" align="center" width="150">
                <template #default="scope">
                    <template v-if="!scope.row.isTenant">
                        <el-button v-permission="['role:write']" size="small" @click="handleEdit(scope.$index, scope.row)"> 编辑</el-button>
                        <el-button v-permission="['role:remove']" size="small" type="danger" @click="handleDelete(scope.$index, scope.row)"> 删除 </el-button>
                    </template>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog v-model="role.dialogVisible" :title="type === 'create' ? '新增角色' : '编辑角色'" width="600" :before-close="handleClose">
            <EditRole v-if="role.dialogVisible" :rules="rules" :roleInfo="role.roleInfo" ref="roleRef" />
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="handleClose">取 消</el-button>
                    <el-button type="primary" @click="saveRole">保存</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>
