<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigLists, updateConfig } from '@/api/config.js'

/**
 * 参数配置：key不可改，value修改即时生效
 */
const configTable = reactive({
    query: { config_key: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getConfigList = () => {
    getConfigLists(configTable.query).then((res) => {
        if (res?.code === 200) {
            configTable.rows = res.data.rows
            configTable.total = res.data.total
        } else {
            ElMessage.error(`获取配置失败！${res?.message}`)
        }
    })
}
getConfigList()

const handleSearch = () => {
    configTable.query.page = 1
    getConfigList()
}
const handleReset = () => {
    configTable.query.config_key = ''
    handleSearch()
}
const handleSizeChange = () => {
    configTable.query.page = 1
    getConfigList()
}

const dialog = reactive({
    visible: false,
    form: { id: null, config_key: '', config_name: '', config_value: '', remark: '' },
})
const formRef = ref()
const rules = reactive({
    config_value: [{ required: true, message: '配置值不能为空', trigger: 'blur' }],
})

const openEdit = (row) => {
    dialog.form = { id: row.id, config_key: row.config_key, config_name: row.config_name, config_value: row.config_value, remark: row.remark }
    dialog.visible = true
}

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        updateConfig({ id: dialog.form.id, config_value: dialog.form.config_value }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '保存成功')
                dialog.visible = false
                getConfigList()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between mb-10px">
            <div class="flex items-center gap-10px">
                <span>配置键：</span>
                <el-input class="w-200px" v-model="configTable.query.config_key" placeholder="请输入配置键" clearable @keyup.enter="handleSearch" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-tag type="info">配置修改后即时生效，无需重启</el-tag>
        </div>
        <el-table :data="configTable.rows" border :max-height="configTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="config_key" label="配置键" min-width="180" />
            <el-table-column prop="config_name" label="配置名称" min-width="160" />
            <el-table-column prop="config_value" label="配置值" min-width="120" align="center">
                <template #default="scope">
                    <el-tag type="success">{{ scope.row.config_value }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column prop="update_by" label="修改人" width="110" align="center" />
            <el-table-column prop="update_time" label="更新时间" width="170" />
            <el-table-column label="操作" width="90" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link v-permission="['config:write']" @click="openEdit(scope.row)">编辑</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="configTable.query.page"
            v-model:page-size="configTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="configTable.total"
            @size-change="handleSizeChange"
            @current-change="getConfigList"
        />

        <el-dialog v-model="dialog.visible" title="修改配置" width="480" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
                <el-form-item label="配置键">
                    <el-input v-model="dialog.form.config_key" disabled />
                </el-form-item>
                <el-form-item label="配置名称">
                    <el-input v-model="dialog.form.config_name" disabled />
                </el-form-item>
                <el-form-item label="配置值" prop="config_value">
                    <el-input v-model="dialog.form.config_value" maxlength="255" />
                </el-form-item>
                <el-form-item label="备注">
                    <span class="text-12px color-#909399">{{ dialog.form.remark || '-' }}</span>
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
