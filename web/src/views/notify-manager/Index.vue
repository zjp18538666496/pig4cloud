<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    createNotifyChannel, createNotifyTemplate, createWebhook, delNotifyChannel, delNotifyTemplate,
    delWebhook, getNotifyChannels, getNotifyLogs, getNotifyTemplates, getWebhookLists,
    testNotifySend, testWebhook, updateNotifyChannel, updateNotifyTemplate, updateWebhook,
} from '@/api/notify.js'

/**
 * 通知管理（平台级）：渠道配置 / 消息模板 / 发送记录，支持测试发送
 */
const activeTab = ref('channel')

// 事件目录（订阅选项）
const EVENT_OPTIONS = [
    { value: 'notice-publish', label: '公告发布' },
    { value: 'job-failed', label: '任务失败' },
    { value: 'tenant-expire-warning', label: '租户到期预警' },
    { value: 'user-quota-warning', label: '用户配额预警' },
    { value: 'approval-pending', label: '审批待办' },
    { value: 'approval-result', label: '审批结果' },
]
const channelTable = reactive({ query: { channel_name: '', page: 1, pageSize: 10 }, rows: [], total: 0 })
const templateTable = reactive({ query: { template_name: '', page: 1, pageSize: 10 }, rows: [], total: 0 })
const logTable = reactive({ query: { channelId: null, success: null, page: 1, pageSize: 10 }, rows: [], total: 0, loading: false })
const webhookTable = reactive({ query: { webhook_name: '', page: 1, pageSize: 10 }, rows: [], total: 0 })
const whLogTable = reactive({ query: { channelId: null, page: 1, pageSize: 10 }, rows: [], total: 0, loading: false })

const loadWebhooks = () => {
    getWebhookLists(webhookTable.query).then((res) => {
        if (res?.code === 200) { webhookTable.rows = res.data.rows; webhookTable.total = res.data.total }
    })
}
const whDialog = reactive({ visible: false, form: {} })
const openWebhook = (row) => {
    whDialog.form = row ? { ...row } : { id: null, webhook_name: '', url: '', secret: '', events: 'notice-publish', status: '1' }
    whDialog.visible = true
}
const saveWebhook = () => {
    whDialog.form.events = whEventList.value.join(',')
    const action = whDialog.form.id ? updateWebhook : createWebhook
    action(whDialog.form).then((res) => {
        if (res?.code === 200) { ElMessage.success(res.message || '保存成功'); whDialog.visible = false; loadWebhooks() } else ElMessage.error(res?.message)
    })
}
const delWebhookRow = (row) => {
    ElMessageBox.confirm(`删除Webhook【${row.webhook_name}】？`, '提示').then(() => {
        delWebhook({ id: row.id }).then((res) => {
            if (res?.code === 200) { ElMessage.success('删除成功'); loadWebhooks() } else ElMessage.error(res?.message)
        })
    })
}
const whTestVisible = reactive({ visible: false, form: { channelId: null, name: '' } })
const openWhTest = (row) => {
    whTestVisible.form = { channelId: row.id, name: row.webhook_name }
    whTestVisible.visible = true
}
const sendWhTest = () => {
    testWebhook({ channelId: whTestVisible.form.channelId }).then((res) => {
        if (res?.code === 200) { ElMessage.success(res.message); whTestVisible.visible = false; loadWhLogs() } else ElMessage.error(res?.message)
    })
}
const loadWhLogs = () => {
    whLogTable.loading = true
    getWebhookLogs(whLogTable.query).then((res) => {
        if (res?.code === 200) { whLogTable.rows = res.data.rows; whLogTable.total = res.data.total }
    }).finally(() => { whLogTable.loading = false })
}
const whEventList = computed({
    get: () => String(whDialog.form.events || '').split(',').filter(Boolean),
    set: (ids) => { whDialog.form.events = (ids || []).join(',') },
})

const eventLabel = (events) => String(events || '').split(',').map(e => (EVENT_OPTIONS.find(o => o.value === e) || {}).label || e).join('、')

const typeOptions = [
    { value: 'email', label: '邮件' },
    { value: 'webhook', label: '通用Webhook' },
    { value: 'dingtalk', label: '钉钉机器人' },
    { value: 'wecom', label: '企业微信机器人' },
    { value: 'feishu', label: '飞书机器人' },
]
const typeLabel = (v) => (typeOptions.find(t => t.value === v) || {}).label || v

const loadChannels = () => {
    getNotifyChannels(channelTable.query).then((res) => {
        if (res?.code === 200) { channelTable.rows = res.data.rows; channelTable.total = res.data.total }
    })
}
const loadTemplates = () => {
    getNotifyTemplates(templateTable.query).then((res) => {
        if (res?.code === 200) { templateTable.rows = res.data.rows; templateTable.total = res.data.total }
    })
}
const loadLogs = () => {
    logTable.loading = true
    getNotifyLogs(logTable.query).then((res) => {
        if (res?.code === 200) { logTable.rows = res.data.rows; logTable.total = res.data.total }
    }).finally(() => { logTable.loading = false })
}
onMounted(() => { loadChannels(); loadTemplates(); loadLogs() })

// 切到事件推送tab时加载
const onTabChange = (name) => {
    if (name === 'webhook' && !webhookTable.rows.length) loadWebhooks()
}

const channelDialog = reactive({ visible: false, form: {} })
const openChannel = (row) => {
    channelDialog.form = row
        ? { ...row }
        : { id: null, channel_name: '', channel_type: 'webhook', config: '{\n  "url": ""\n}', status: '1', remark: '' }
    channelDialog.visible = true
}
const saveChannel = () => {
    const action = channelDialog.form.id ? updateNotifyChannel : createNotifyChannel
    action(channelDialog.form).then((res) => {
        if (res?.code === 200) {
            ElMessage.success(res.message || '保存成功')
            channelDialog.visible = false
            loadChannels()
        } else ElMessage.error(res?.message)
    })
}
const delChannel = (row) => {
    ElMessageBox.confirm(`删除渠道【${row.channel_name}】？`, '提示').then(() => {
        delNotifyChannel({ id: row.id }).then((res) => {
            if (res?.code === 200) { ElMessage.success('删除成功'); loadChannels() } else ElMessage.error(res?.message)
        })
    })
}
const testVisible = reactive({ visible: false, form: { channelId: null, channelName: '', title: '', content: '' } })
const openTest = (row) => {
    testVisible.form = { channelId: row.id, channelName: row.channel_name, title: '', content: '' }
    testVisible.visible = true
}
const sendTest = () => {
    testNotifySend(testVisible.form).then((res) => {
        if (res?.code === 200) { ElMessage.success(res.message); testVisible.visible = false; loadLogs() } else ElMessage.error(res?.message)
    })
}

const templateDialog = reactive({ visible: false, form: {} })
const openTemplate = (row) => {
    templateDialog.form = row ? { ...row } : { id: null, template_code: '', template_name: '', title_template: '', content_template: '', status: '1', remark: '' }
    templateDialog.visible = true
}
const saveTemplate = () => {
    const action = templateDialog.form.id ? updateNotifyTemplate : createNotifyTemplate
    action(templateDialog.form).then((res) => {
        if (res?.code === 200) {
            ElMessage.success(res.message || '保存成功')
            templateDialog.visible = false
            loadTemplates()
        } else ElMessage.error(res?.message)
    })
}
const delTemplate = (row) => {
    ElMessageBox.confirm(`删除模板【${row.template_name}】？`, '提示').then(() => {
        delNotifyTemplate({ id: row.id }).then((res) => {
            if (res?.code === 200) { ElMessage.success('删除成功'); loadTemplates() } else ElMessage.error(res?.message)
        })
    })
}
</script>

<template>
    <div class="page">
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <el-tab-pane label="通知渠道" name="channel">
                <div class="flex justify-between mb-10px">
                    <el-input class="w-200px" v-model="channelTable.query.channel_name" placeholder="渠道名称" clearable @keyup.enter="loadChannels" />
                    <el-button type="primary" @click="openChannel()">新增渠道</el-button>
                </div>
                <el-table :data="channelTable.rows" border>
                    <el-table-column prop="channel_name" label="名称" min-width="120" />
                    <el-table-column label="类型" width="130" align="center">
                        <template #default="scope">{{ typeLabel(scope.row.channel_type) }}</template>
                    </el-table-column>
                    <el-table-column prop="config" label="配置" min-width="220" show-overflow-tooltip />
                    <el-table-column label="状态" width="80" align="center">
                        <template #default="scope">
                            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">{{ scope.row.status === '1' ? '启用' : '停用' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
                    <el-table-column label="操作" width="180" align="center">
                        <template #default="scope">
                            <el-button size="small" type="success" link @click="openTest(scope.row)">测试</el-button>
                            <el-button size="small" type="primary" link @click="openChannel(scope.row)">编辑</el-button>
                            <el-button size="small" type="danger" link @click="delChannel(scope.row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <el-pagination class="mt-10px flex justify-end" v-model:current-page="channelTable.query.page"
                    v-model:page-size="channelTable.query.pageSize" :total="channelTable.total" layout="total, prev, pager, next"
                    @current-change="loadChannels" />
            </el-tab-pane>

            <el-tab-pane label="消息模板" name="template">
                <div class="flex justify-between mb-10px">
                    <el-input class="w-200px" v-model="templateTable.query.template_name" placeholder="模板名称" clearable @keyup.enter="loadTemplates" />
                    <el-button type="primary" @click="openTemplate()">新增模板</el-button>
                </div>
                <el-table :data="templateTable.rows" border>
                    <el-table-column prop="template_code" label="编码" min-width="140" />
                    <el-table-column prop="template_name" label="名称" min-width="120" />
                    <el-table-column prop="title_template" label="标题模板" min-width="180" show-overflow-tooltip />
                    <el-table-column prop="content_template" label="内容模板" min-width="240" show-overflow-tooltip />
                    <el-table-column label="状态" width="80" align="center">
                        <template #default="scope">
                            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">{{ scope.row.status === '1' ? '启用' : '停用' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="120" align="center">
                        <template #default="scope">
                            <el-button size="small" type="primary" link @click="openTemplate(scope.row)">编辑</el-button>
                            <el-button size="small" type="danger" link @click="delTemplate(scope.row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <el-pagination class="mt-10px flex justify-end" v-model:current-page="templateTable.query.page"
                    v-model:page-size="templateTable.query.pageSize" :total="templateTable.total" layout="total, prev, pager, next"
                    @current-change="loadTemplates" />
            </el-tab-pane>


            <el-tab-pane label="事件推送" name="webhook">
                <div class="flex justify-between mb-10px">
                    <el-input class="w-200px" v-model="webhookTable.query.webhook_name" placeholder="名称" clearable @keyup.enter="loadWebhooks" />
                    <el-button type="primary" @click="openWebhook()">新增Webhook</el-button>
                </div>
                <el-table :data="webhookTable.rows" border>
                    <el-table-column prop="webhook_name" label="名称" min-width="120" />
                    <el-table-column prop="url" label="接收地址" min-width="220" show-overflow-tooltip />
                    <el-table-column label="订阅事件" min-width="200">
                        <template #default="scope">{{ eventLabel(scope.row.events) }}</template>
                    </el-table-column>
                    <el-table-column label="签名" width="70" align="center">
                        <template #default="scope">
                            <el-tag :type="scope.row.secret ? 'success' : 'info'" size="small">{{ scope.row.secret ? '有' : '无' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="80" align="center">
                        <template #default="scope">
                            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">{{ scope.row.status === '1' ? '启用' : '停用' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="180" align="center">
                        <template #default="scope">
                            <el-button size="small" type="success" link @click="openWhTest(scope.row)">测试</el-button>
                            <el-button size="small" type="primary" link @click="openWebhook(scope.row)">编辑</el-button>
                            <el-button size="small" type="danger" link @click="delWebhookRow(scope.row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <el-pagination class="mt-10px flex justify-end" v-model:current-page="webhookTable.query.page"
                    v-model:page-size="webhookTable.query.pageSize" :total="webhookTable.total" layout="total, prev, pager, next"
                    @current-change="loadWebhooks" />
            </el-tab-pane>

            <el-tab-pane label="发送记录" name="log">
                <div class="flex items-center gap-10px mb-10px">
                    <el-select v-model="logTable.query.success" placeholder="全部结果" clearable style="width: 120px" @change="logTable.query.page = 1; loadLogs()">
                        <el-option label="成功" :value="true" />
                        <el-option label="失败" :value="false" />
                    </el-select>
                    <el-button @click="loadLogs">刷新</el-button>
                </div>
                <el-table :data="logTable.rows" border v-loading="logTable.loading">
                    <el-table-column prop="createTime" label="时间" width="165" />
                    <el-table-column prop="channelName" label="渠道" min-width="110" />
                    <el-table-column prop="channelType" label="类型" width="90" align="center" />
                    <el-table-column prop="templateCode" label="模板" width="140" />
                    <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
                    <el-table-column label="结果" width="70" align="center">
                        <template #default="scope">
                            <el-tag :type="scope.row.success ? 'success' : 'danger'" size="small">{{ scope.row.success ? '成功' : '失败' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="message" label="说明" min-width="140" show-overflow-tooltip />
                    <el-table-column prop="receiver" label="接收目标" min-width="160" show-overflow-tooltip />
                    <el-table-column prop="costMs" label="耗时(ms)" width="85" align="center" />
                </el-table>
                <el-pagination class="mt-10px flex justify-end" v-model:current-page="logTable.query.page"
                    v-model:page-size="logTable.query.pageSize" :total="logTable.total" layout="total, prev, pager, next"
                    @current-change="loadLogs" />
            </el-tab-pane>
        </el-tabs>

        <el-dialog v-model="channelDialog.visible" :title="channelDialog.form.id ? '编辑渠道' : '新增渠道'" width="560">
            <el-form label-width="90px">
                <el-form-item label="渠道名称">
                    <el-input v-model="channelDialog.form.channel_name" placeholder="如：运维钉钉群" maxlength="64" />
                </el-form-item>
                <el-form-item label="类型">
                    <el-select v-model="channelDialog.form.channel_type" style="width: 100%">
                        <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="配置JSON">
                    <el-input v-model="channelDialog.form.config" type="textarea" :rows="4" placeholder='email: {"to":"a@x.com"}；webhook/钉钉/企微/飞书: {"url":"...","secret":"钉钉加签密钥"}' />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="channelDialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="channelDialog.form.remark" maxlength="255" />
                </el-form-item>
                <el-alert type="info" :closable="false"
                    title="钉钉/企微/飞书填群机器人的Webhook地址；钉钉加签方式需同时填secret。邮件渠道需服务端已配置spring.mail.*。保存后点“测试”验证连通性" />
            </el-form>
            <template #footer>
                <el-button @click="channelDialog.visible = false">取消</el-button>
                <el-button type="primary" @click="saveChannel">保存</el-button>
            </template>
        </el-dialog>


        <el-dialog v-model="whDialog.visible" :title="whDialog.form.id ? '编辑Webhook' : '新增Webhook'" width="560">
            <el-form label-width="90px">
                <el-form-item label="名称">
                    <el-input v-model="whDialog.form.webhook_name" maxlength="64" placeholder="如：ERP系统接收端" />
                </el-form-item>
                <el-form-item label="接收地址">
                    <el-input v-model="whDialog.form.url" placeholder="https://..." maxlength="500" />
                </el-form-item>
                <el-form-item label="订阅事件">
                    <el-select v-model="whEventList" multiple style="width: 100%" placeholder="选择要接收的事件">
                        <el-option v-for="e in EVENT_OPTIONS" :key="e.value" :label="e.label" :value="e.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="签名密钥">
                    <el-input v-model="whDialog.form.secret" maxlength="64" placeholder="可选；HMAC-SHA256(timestamp+body)" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="whDialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-alert type="info" :closable="false"
                    title="POST JSON {event,data,timestamp}；配了密钥则附带X-Timestamp/X-Signature头（HMAC-SHA256），失败自动重试3次" />
            </el-form>
            <template #footer>
                <el-button @click="whDialog.visible = false">取消</el-button>
                <el-button type="primary" @click="saveWebhook">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="whTestVisible.visible" :title="`测试推送：${whTestVisible.form.name || ''}`" width="420">
            <div class="text-13px" style="color:#909399">将发送一条 test 事件（含签名与时间戳）到该地址，投递记录可在发送记录tab查看。</div>
            <template #footer>
                <el-button @click="whTestVisible.visible = false">取消</el-button>
                <el-button type="primary" @click="sendWhTest">发送</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="templateDialog.visible" :title="templateDialog.form.id ? '编辑模板' : '新增模板'" width="560">
            <el-form label-width="90px">
                <el-form-item label="模板编码">
                    <el-input v-model="templateDialog.form.template_code" :disabled="!!templateDialog.form.id" placeholder="如 notice-publish" />
                </el-form-item>
                <el-form-item label="模板名称">
                    <el-input v-model="templateDialog.form.template_name" maxlength="64" />
                </el-form-item>
                <el-form-item label="标题模板">
                    <el-input v-model="templateDialog.form.title_template" placeholder="支持${变量}，如 新公告：${title}" />
                </el-form-item>
                <el-form-item label="内容模板">
                    <el-input v-model="templateDialog.form.content_template" type="textarea" :rows="4" placeholder="支持${变量}" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="templateDialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="templateDialog.visible = false">取消</el-button>
                <el-button type="primary" @click="saveTemplate">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="testVisible.visible" :title="`测试发送：${testVisible.form.channelName || ''}`" width="480">
            <el-form label-width="70px">
                <el-form-item label="标题">
                    <el-input v-model="testVisible.form.title" placeholder="默认：PIGX ADMIN 通知测试" />
                </el-form-item>
                <el-form-item label="内容">
                    <el-input v-model="testVisible.form.content" type="textarea" :rows="3" placeholder="默认：这是一条测试通知" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="testVisible.visible = false">取消</el-button>
                <el-button type="primary" @click="sendTest">发送</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
