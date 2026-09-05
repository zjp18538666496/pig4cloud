<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createApiKey, delApiKey, getApiKeyLists, getApiKeyLogs, updateApiKey } from '@/api/api-key.js'

/**
 * Open API密钥管理（仅超管）：创建时完整密钥只展示一次
 */
const keyTable = reactive({
    query: { app_name: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getList = () => {
    getApiKeyLists(keyTable.query).then((res) => {
        if (res?.code === 200) {
            keyTable.rows = res.data.rows
            keyTable.total = res.data.total
        } else {
            ElMessage.error(`获取密钥失败！${res?.message}`)
        }
    })
}
getList()

const handleSearch = () => {
    keyTable.query.page = 1
    getList()
}
const handleReset = () => {
    keyTable.query.app_name = ''
    handleSearch()
}
const handleSizeChange = () => {
    keyTable.query.page = 1
    getList()
}

const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, app_name: '', scopes: '', status: '1', remark: '', expire_time: null },
})
const formRef = ref()
const rules = reactive({
    app_name: [{ required: true, message: '请输入接入方名称', trigger: 'blur' }],
    scopes: [{ required: true, message: '请输入授权范围', trigger: 'blur' }],
})

const openDialog = (type, row) => {
    dialog.type = type
    dialog.form = type === 'create'
        ? { id: null, app_name: '', scopes: 'user:read', status: '1', remark: '', expire_time: null }
        : { id: row.id, app_name: row.app_name, scopes: row.scopes, status: row.status, remark: row.remark, expire_time: row.expire_time }
    dialog.visible = true
}

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        const action = dialog.type === 'create' ? createApiKey : updateApiKey
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                if (dialog.type === 'create' && res.data?.api_key) {
                    showKeyOnce(res.data.api_key, res.data.api_secret)
                } else {
                    ElMessage.success(res.message || '保存成功')
                }
                dialog.visible = false
                getList()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}

const showKeyOnce = (key, secret) => {
    ElMessageBox.alert(
        `<div style="word-break:break-all;font-family:monospace;font-size:15px;user-select:all">Key: ${key}</div>` +
        `<div style="word-break:break-all;font-family:monospace;font-size:15px;user-select:all;margin-top:6px">Secret: ${secret || '（未生成，simple模式调用无需Secret）'}</div>` +
        '<div style="margin-top:8px;color:#f53f3f;font-size:12px">Key与Secret仅展示这一次，请立即复制保存！' +
        'simple模式：请求头携带X-Api-Key即可；hmac签名模式：另需X-Timestamp/X-Nonce/X-Signature（HMAC-SHA256，签名串详见README）</div>',
        '密钥创建成功',
        { dangerouslyUseHTMLString: true, confirmButtonText: '我已保存', type: 'warning' },
    )
}

/**
 * 调用日志抽屉
 */
const logDrawer = reactive({ visible: false, rows: [], total: 0, loading: false,
    query: { keyId: null, success: null, page: 1, pageSize: 10 } })

const openLogs = (row) => {
    logDrawer.query.keyId = row.id
    logDrawer.query.success = null
    logDrawer.query.page = 1
    logDrawer.visible = true
    loadLogs()
}

const loadLogs = () => {
    logDrawer.loading = true
    getApiKeyLogs(logDrawer.query).then((res) => {
        if (res?.code === 200) {
            logDrawer.rows = res.data.rows
            logDrawer.total = res.data.total
        }
    }).finally(() => {
        logDrawer.loading = false
    })
}

const copyKey = (key) => {
    navigator.clipboard?.writeText(key)
    ElMessage.success('已复制')
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`删除后接入方【${row.app_name}】将立即无法调用，您确定删除吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        delApiKey({ id: row.id }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '删除成功')
                getList()
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
                <span>接入方：</span>
                <el-input class="w-200px" v-model="keyTable.query.app_name" placeholder="请输入接入方名称" clearable @keyup.enter="handleSearch" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" v-permission="['apikey:manage']" @click="openDialog('create')">创建密钥</el-button>
        </div>
        <el-table :data="keyTable.rows" border :max-height="keyTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="app_name" label="接入方" min-width="140" />
            <el-table-column label="API Key" min-width="240">
                <template #default="scope">
                    <span class="key-text" :title="scope.row.api_key">{{ scope.row.api_key }}</span>
                    <el-button size="small" link type="primary" @click="copyKey(scope.row.api_key)">复制</el-button>
                </template>
            </el-table-column>
            <el-table-column prop="scopes" label="授权范围" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">{{ scope.row.status === '1' ? '启用' : '停用' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="有效期至" width="170" align="center">
                <template #default="scope">{{ scope.row.expire_time || '永久' }}</template>
            </el-table-column>
            <el-table-column prop="last_used_time" label="最后调用" width="170" align="center">
                <template #default="scope">{{ scope.row.last_used_time || '从未调用' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link @click="openLogs(scope.row)">调用日志</el-button>
                    <el-button size="small" type="primary" link v-permission="['apikey:manage']" @click="openDialog('edit', scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link v-permission="['apikey:manage']" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="keyTable.query.page"
            v-model:page-size="keyTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="keyTable.total"
            @size-change="handleSizeChange"
            @current-change="getList"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '创建密钥' : '编辑密钥'" width="520" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
                <el-form-item label="接入方" prop="app_name">
                    <el-input v-model="dialog.form.app_name" placeholder="如：OA系统" maxlength="64" />
                </el-form-item>
                <el-form-item label="授权范围" prop="scopes">
                    <el-input v-model="dialog.form.scopes" placeholder="逗号分隔，如 user:read,notice:read" maxlength="255" />
                </el-form-item>
                <el-form-item label="有效期至">
                    <el-date-picker v-model="dialog.form.expire_time" type="datetime" placeholder="不填为永久" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
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
                <el-alert type="info" :closable="false"
                    title="调用方式：GET /api/open/v1/users、/api/open/v1/notices，请求头携带 X-Api-Key；可用scope：user:read、notice:read。默认both模式，请求头额外携带X-Timestamp/X-Nonce/X-Signature即自动启用HMAC验签（防冒充/防重放）" />
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>

        <el-drawer v-model="logDrawer.visible" title="Open API调用日志" size="900">
            <div class="flex items-center gap-10px mb-10px">
                <el-select v-model="logDrawer.query.success" placeholder="全部结果" clearable style="width: 120px" @change="logDrawer.query.page = 1; loadLogs()">
                    <el-option label="成功" :value="true" />
                    <el-option label="失败" :value="false" />
                </el-select>
                <el-button @click="loadLogs">刷新</el-button>
            </div>
            <el-table :data="logDrawer.rows" border size="small" v-loading="logDrawer.loading">
                <el-table-column prop="createTime" label="时间" width="165" />
                <el-table-column prop="appName" label="接入方" width="110" show-overflow-tooltip />
                <el-table-column prop="method" label="方式" width="60" align="center" />
                <el-table-column prop="path" label="接口" min-width="150" show-overflow-tooltip />
                <el-table-column prop="query" label="参数" min-width="120" show-overflow-tooltip />
                <el-table-column label="结果" width="70" align="center">
                    <template #default="scope">
                        <el-tag :type="scope.row.success ? 'success' : 'danger'" size="small">{{ scope.row.success ? '成功' : '失败' }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="message" label="失败原因" min-width="110" show-overflow-tooltip />
                <el-table-column prop="ip" label="来源IP" width="120" />
                <el-table-column prop="region" label="归属地" width="130" show-overflow-tooltip />
                <el-table-column prop="costMs" label="耗时(ms)" width="80" align="center" />
            </el-table>
            <el-pagination
                class="mt-10px flex justify-end"
                v-model:current-page="logDrawer.query.page"
                v-model:page-size="logDrawer.query.pageSize"
                :background="true"
                layout="total, prev, pager, next"
                :total="logDrawer.total"
                @current-change="loadLogs"
            />
        </el-drawer>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}

.key-text {
    font-family: monospace;
    font-size: 12px;
}
</style>
