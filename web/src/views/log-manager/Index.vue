<template>
    <div>
        <el-tabs v-model="activeTab">
            <el-tab-pane label="操作日志" name="operate">
                <div class="mb-20px flex justify-between items-center">
                    <div>
                        操作人
                        <el-input
                            v-model="operateTable.query.username"
                            class="w240px"
                            placeholder="操作人(模糊)"
                            @keyup.enter="handleOperateSearch"
                        />
                        <el-button class="ml-10px" @click="handleOperateSearch">查询</el-button>
                        <el-button type="primary" @click="handleOperateReset">重置</el-button>
                    </div>
                    <el-button type="success" plain @click="handleExport">导出</el-button>
                </div>
                <el-table :data="operateTable.rows" border style="width: 100%" :max-height="logTableHeight">
                    <el-table-column prop="createTime" label="时间" width="170" align="center" />
                    <el-table-column prop="username" label="操作人" width="110" align="center" />
                    <el-table-column prop="module" label="模块" width="100" align="center" />
                    <el-table-column prop="operation" label="操作" width="100" align="center" />
                    <el-table-column prop="method" label="方式" width="70" align="center" />
                    <el-table-column prop="url" label="接口地址" align="left" show-overflow-tooltip />
                    <el-table-column prop="params" label="请求参数" align="left" show-overflow-tooltip />
                    <el-table-column prop="ip" label="来源IP" width="130" align="center" />
                    <el-table-column prop="success" label="结果" width="70" align="center">
                        <template #default="scope">
                            <el-tag :type="scope.row.success ? 'success' : 'danger'">
                                {{ scope.row.success ? '成功' : '失败' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="costMs" label="耗时(ms)" width="85" align="center" />
                    <el-table-column prop="errorMsg" label="失败信息" align="left" show-overflow-tooltip />
                    <el-table-column prop="diff" label="变更对比" align="left" width="200">
                        <template #default="scope">
                            <el-tooltip v-if="scope.row.diff" :content="scope.row.diff" placement="top">
                                <span class="diff-cell">{{ scope.row.diff }}</span>
                            </el-tooltip>
                            <span v-else>-</span>
                        </template>
                    </el-table-column>
                </el-table>
                <el-pagination
                    v-model:current-page="operateTable.query.page"
                    v-model:page-size="operateTable.query.pageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="operateTable.total"
                    layout="total, sizes, prev, pager, next, jumper"
                    class="mt-10px justify-end"
                    @current-change="getOperateList"
                    @size-change="handleOperateSizeChange"
                />
            </el-tab-pane>
            <el-tab-pane label="登录日志" name="login">
                <div class="mb-20px flex justify-between items-center">
                    <div>
                        操作人
                        <el-input
                            v-model="loginTable.query.username"
                            class="w240px"
                            placeholder="操作人(模糊)"
                            @keyup.enter="handleLoginSearch"
                        />
                        <el-select v-model="loginTable.query.success" class="ml-10px w120px" placeholder="结果" clearable>
                            <el-option label="成功" :value="true" />
                            <el-option label="失败" :value="false" />
                        </el-select>
                        <el-button class="ml-10px" @click="handleLoginSearch">查询</el-button>
                        <el-button type="primary" @click="handleLoginReset">重置</el-button>
                    </div>
                    <el-button type="success" plain @click="handleExport">导出</el-button>
                </div>
                <el-table :data="loginTable.rows" border style="width: 100%" :max-height="logTableHeight">
                    <el-table-column prop="createTime" label="时间" width="170" align="center" />
                    <el-table-column prop="username" label="操作人" width="130" align="center" />
                    <el-table-column prop="ip" label="来源IP" width="150" align="center" />
                    <el-table-column prop="success" label="结果" width="90" align="center">
                        <template #default="scope">
                            <el-tag :type="scope.row.success ? 'success' : 'danger'">
                                {{ scope.row.success ? '成功' : '失败' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="message" label="说明" align="left" show-overflow-tooltip />
                </el-table>
                <el-pagination
                    v-model:current-page="loginTable.query.page"
                    v-model:page-size="loginTable.query.pageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="loginTable.total"
                    layout="total, sizes, prev, pager, next, jumper"
                    class="mt-10px justify-end"
                    @current-change="getLoginList"
                    @size-change="handleLoginSizeChange"
                />
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportLoginLogs, exportOperateLogs, getLoginLogs, getOperateLogs } from '@/api/log.js'

const activeTab = ref('operate')
const logTableHeight = window.innerHeight - 50 - 30 - 40 - 90

const downloadBlob = (blob, filename) => {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
}

const handleExport = () => {
    const isOperate = activeTab.value === 'operate'
    const request = isOperate ? exportOperateLogs(operateTable.query) : exportLoginLogs(loginTable.query)
    request.then((res) => {
        if (res instanceof Blob) {
            downloadBlob(res, isOperate ? '操作日志.xlsx' : '登录日志.xlsx')
        } else {
            ElMessage.error('导出失败，请重试')
        }
    })
}

const operateTable = reactive({
    query: { username: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
})

const getOperateList = () => {
    getOperateLogs(operateTable.query).then((res) => {
        if (res?.code === 200) {
            operateTable.rows = res.data.rows
            operateTable.total = res.data.total
        }
    })
}

const handleOperateSearch = () => {
    operateTable.query.page = 1
    getOperateList()
}

const handleOperateReset = () => {
    operateTable.query.username = ''
    handleOperateSearch()
}

const handleOperateSizeChange = () => {
    operateTable.query.page = 1
    getOperateList()
}

const loginTable = reactive({
    query: { username: '', success: null, page: 1, pageSize: 10 },
    rows: [],
    total: 0,
})

const getLoginList = () => {
    getLoginLogs(loginTable.query).then((res) => {
        if (res?.code === 200) {
            loginTable.rows = res.data.rows
            loginTable.total = res.data.total
        }
    })
}

const handleLoginSearch = () => {
    loginTable.query.page = 1
    getLoginList()
}

const handleLoginReset = () => {
    loginTable.query.username = ''
    loginTable.query.success = null
    handleLoginSearch()
}

const handleLoginSizeChange = () => {
    loginTable.query.page = 1
    getLoginList()
}

onMounted(getOperateList)
</script>

<style scoped>
.diff-cell {
    max-width: 190px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: inline-block;
    font-family: monospace;
    font-size: 12px;
    cursor: help;
}
</style>
