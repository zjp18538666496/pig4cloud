<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPackage, delPackage, getPackageLists, updatePackage } from '@/api/package.js'
import { getMenuLists } from '@/api/menu.js'

/**
 * 租户套餐管理：套餐决定开通租户时租户管理员角色的菜单范围
 */
const packageTable = reactive({
    query: { package_name: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getPackageList = () => {
    getPackageLists(packageTable.query).then((res) => {
        if (res?.code === 200) {
            packageTable.rows = res.data.rows
            packageTable.total = res.data.total
        } else {
            ElMessage.error(`获取套餐失败！${res?.message}`)
        }
    })
}
getPackageList()

const handleSearch = () => {
    packageTable.query.page = 1
    getPackageList()
}

const handleReset = () => {
    packageTable.query.package_name = ''
    handleSearch()
}

const handleSizeChange = () => {
    packageTable.query.page = 1
    getPackageList()
}

const statusText = (status) => ({ '0': '停用', '1': '启用' }[status] || status)

/**
 * 新增/编辑弹窗
 */
const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, package_name: '', menu_codes: [], status: '1', remark: '' },
})
const formRef = ref()
const rules = reactive({
    package_name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
})

// 菜单树（与角色管理的菜单勾选一致）
const menuList = ref([])
const menuTreeProps = {
    children: 'children',
    label: 'menu_name',
    value: 'id',
}
getMenuLists({}).then((res) => {
    if (res?.code === 200) {
        menuList.value = res.data
    }
})

const openDialog = (type, row) => {
    dialog.type = type
    if (type === 'create') {
        dialog.form = { id: null, package_name: '', menu_codes: [], status: '1', remark: '' }
    } else {
        // menu_ids为逗号分隔的菜单id字符串，拆回数组供树勾选
        const menuIds = (row.menu_ids || '').split(',').filter((item) => item !== '').map(Number)
        dialog.form = { id: row.id, package_name: row.package_name, menu_codes: menuIds, status: row.status, remark: row.remark }
    }
    dialog.visible = true
}

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) {
            return
        }
        if (!dialog.form.menu_codes.length) {
            ElMessage.warning('请至少勾选一个菜单')
            return
        }
        const action = dialog.type === 'create' ? createPackage : updatePackage
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '保存成功')
                dialog.visible = false
                getPackageList()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`您确定删除套餐【${row.package_name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        delPackage({ id: row.id }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '删除成功')
                getPackageList()
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
                <span>套餐名称(模糊)：</span>
                <el-input class="w-200px" v-model="packageTable.query.package_name" placeholder="请输入套餐名称" clearable @keyup.enter="handleSearch" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" v-permission="['package:write']" @click="openDialog('create')">新增套餐</el-button>
        </div>
        <el-table :data="packageTable.rows" border :max-height="packageTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="package_name" label="套餐名称" min-width="140" />
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
            <el-table-column label="菜单数量" width="100" align="center">
                <template #default="scope">
                    {{ (scope.row.menu_ids || '').split(',').filter((item) => item !== '').length }}
                </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">{{ statusText(scope.row.status) }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="create_time" label="创建时间" width="170" />
            <el-table-column label="操作" width="150" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link v-permission="['package:write']" @click="openDialog('edit', scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link v-permission="['package:remove']" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="packageTable.query.page"
            v-model:page-size="packageTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="packageTable.total"
            @size-change="handleSizeChange"
            @current-change="getPackageList"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '新增套餐' : '编辑套餐'" width="560" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
                <el-form-item label="套餐名称" prop="package_name">
                    <el-input v-model="dialog.form.package_name" placeholder="请输入套餐名称" maxlength="64" />
                </el-form-item>
                <el-form-item label="菜单权限">
                    <el-tree-select
                        v-model="dialog.form.menu_codes"
                        :data="menuList"
                        :props="menuTreeProps"
                        node-key="id"
                        multiple
                        collapse-tags
                        show-checkbox
                        :check-strictly="false"
                        :render-after-expand="false"
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="dialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="dialog.form.remark" type="textarea" :rows="2" placeholder="套餐说明（可选）" maxlength="255" />
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
