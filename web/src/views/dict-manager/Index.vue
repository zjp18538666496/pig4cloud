<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDict, delDict, getDictItems, getDictLists, saveDictItems, updateDict } from '@/api/dict.js'

/**
 * 字典管理：字典列表 + 字典项维护抽屉
 */
const dictTable = reactive({
    query: { dict_name: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getDictList = () => {
    getDictLists(dictTable.query).then((res) => {
        if (res?.code === 200) {
            dictTable.rows = res.data.rows
            dictTable.total = res.data.total
        } else {
            ElMessage.error(`获取字典失败！${res?.message}`)
        }
    })
}
getDictList()

const handleSearch = () => {
    dictTable.query.page = 1
    getDictList()
}
const handleReset = () => {
    dictTable.query.dict_name = ''
    handleSearch()
}
const handleSizeChange = () => {
    dictTable.query.page = 1
    getDictList()
}

/**
 * 字典新增/编辑弹窗
 */
const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, dict_code: '', dict_name: '', status: '1', remark: '' },
})
const formRef = ref()
const rules = reactive({
    dict_code: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
    dict_name: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
})

const openDialog = (type, row) => {
    dialog.type = type
    dialog.form = type === 'create'
        ? { id: null, dict_code: '', dict_name: '', status: '1', remark: '' }
        : { id: row.id, dict_code: row.dict_code, dict_name: row.dict_name, status: row.status, remark: row.remark }
    dialog.visible = true
}

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        const action = dialog.type === 'create' ? createDict : updateDict
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '保存成功')
                dialog.visible = false
                getDictList()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`删除字典【${row.dict_name}】会同时删除其全部字典项，您确定吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        delDict({ id: row.id }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '删除成功')
                getDictList()
            } else {
                ElMessage.error(`删除失败！${res?.message}`)
            }
        })
    })
}

/**
 * 字典项维护抽屉
 */
const itemDrawer = reactive({
    visible: false,
    dictId: null,
    dictName: '',
    items: [],
})
const openItems = (row) => {
    itemDrawer.dictId = row.id
    itemDrawer.dictName = row.dict_name
    getDictItems(row.id).then((res) => {
        if (res?.code === 200) {
            itemDrawer.items = res.data || []
            itemDrawer.visible = true
        } else {
            ElMessage.error(`获取字典项失败！${res?.message}`)
        }
    })
}
const addItem = () => {
    itemDrawer.items.push({ label: '', value: '', sort: itemDrawer.items.length + 1, status: '1' })
}
const removeItem = (index) => {
    itemDrawer.items.splice(index, 1)
}
const saveItems = () => {
    const valid = itemDrawer.items.every((item) => item.label && item.value)
    if (!valid) {
        ElMessage.warning('字典项的标签和键值不能为空')
        return
    }
    saveDictItems(itemDrawer.dictId, itemDrawer.items).then((res) => {
        if (res?.code === 200) {
            ElMessage.success(res.message || '保存成功')
            itemDrawer.visible = false
        } else {
            ElMessage.error(`保存失败！${res?.message}`)
        }
    })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between mb-10px">
            <div class="flex items-center gap-10px">
                <span>字典名称：</span>
                <el-input class="w-200px" v-model="dictTable.query.dict_name" placeholder="请输入字典名称" clearable @keyup.enter="handleSearch" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" v-permission="['dict:write']" @click="openDialog('create')">新增字典</el-button>
        </div>
        <el-table :data="dictTable.rows" border :max-height="dictTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="dict_code" label="字典编码" min-width="140" />
            <el-table-column prop="dict_name" label="字典名称" min-width="140" />
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">{{ scope.row.status === '1' ? '启用' : '停用' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column prop="update_time" label="更新时间" width="170" />
            <el-table-column label="操作" width="220" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link v-permission="['dict:write']" @click="openItems(scope.row)">字典项</el-button>
                    <el-button size="small" type="primary" link v-permission="['dict:write']" @click="openDialog('edit', scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link v-permission="['dict:remove']" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="dictTable.query.page"
            v-model:page-size="dictTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="dictTable.total"
            @size-change="handleSizeChange"
            @current-change="getDictList"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '新增字典' : '编辑字典'" width="480" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
                <el-form-item label="字典编码" prop="dict_code">
                    <el-input v-model="dialog.form.dict_code" :disabled="dialog.type === 'edit'" placeholder="如gender" maxlength="64" />
                </el-form-item>
                <el-form-item label="字典名称" prop="dict_name">
                    <el-input v-model="dialog.form.dict_name" maxlength="64" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="dialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="dialog.form.remark" type="textarea" :rows="2" maxlength="255" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>

        <el-drawer v-model="itemDrawer.visible" :title="`字典项 - ${itemDrawer.dictName}`" size="560">
            <el-table :data="itemDrawer.items" border size="small">
                <el-table-column label="标签" min-width="120">
                    <template #default="scope">
                        <el-input v-model="scope.row.label" placeholder="如：男" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="键值" min-width="100">
                    <template #default="scope">
                        <el-input v-model="scope.row.value" placeholder="如：1" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="排序" width="90">
                    <template #default="scope">
                        <el-input-number v-model="scope.row.sort" :min="0" size="small" controls-position="right" style="width: 70px" />
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="scope">
                        <el-switch v-model="scope.row.status" active-value="1" inactive-value="0" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="70" align="center">
                    <template #default="scope">
                        <el-button size="small" type="danger" link @click="removeItem(scope.$index)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div class="mt-10px">
                <el-button type="primary" plain size="small" @click="addItem">添加字典项</el-button>
            </div>
            <template #footer>
                <el-button @click="itemDrawer.visible = false">取消</el-button>
                <el-button type="primary" v-permission="['dict:write']" @click="saveItems">保存</el-button>
            </template>
        </el-drawer>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
