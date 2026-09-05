<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadExportTask, getExportTasks } from '@/api/export.js'

/**
 * 导出中心：异步导出任务列表与下载
 */
const taskTable = reactive({ query: { page: 1, pageSize: 10 }, rows: [], total: 0, loading: false })

const loadTasks = () => {
    taskTable.loading = true
    getExportTasks(taskTable.query).then((res) => {
        if (res?.code === 200) { taskTable.rows = res.data.rows; taskTable.total = res.data.total }
    }).finally(() => { taskTable.loading = false })
}
onMounted(loadTasks)

const statusTag = (s) => (s === '1' ? 'success' : s === '2' ? 'danger' : 'warning')
const statusText = (s) => (s === '1' ? '成功' : s === '2' ? '失败' : '处理中')

const handleDownload = (row) => {
    downloadExportTask(row.id).then((res) => {
        if (res instanceof Blob) {
            const url = URL.createObjectURL(res)
            const a = document.createElement('a')
            a.href = url
            a.download = row.title + '.xlsx'
            a.click()
            URL.revokeObjectURL(url)
        } else {
            ElMessage.error('下载失败')
        }
    })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between mb-10px">
            <span class="text-14px" style="color:#909399">异步导出任务列表，生成完成后可下载（保留在当前文件存储中）</span>
            <el-button type="primary" @click="loadTasks">刷新</el-button>
        </div>
        <el-table :data="taskTable.rows" border v-loading="taskTable.loading">
            <el-table-column prop="id" label="任务ID" width="80" align="center" />
            <el-table-column prop="title" label="任务" min-width="200" />
            <el-table-column prop="task_type" label="类型" width="100" align="center" />
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="statusTag(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="total" label="行数" width="90" align="center" />
            <el-table-column prop="message" label="失败原因" min-width="150" show-overflow-tooltip />
            <el-table-column prop="create_by" label="提交人" width="120" />
            <el-table-column prop="create_time" label="提交时间" width="170" align="center" />
            <el-table-column prop="finish_time" label="完成时间" width="170" align="center" />
            <el-table-column label="操作" width="100" align="center">
                <template #default="scope">
                    <el-button v-if="scope.row.status === '1'" size="small" type="primary" @click="handleDownload(scope.row)">下载</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination class="mt-10px flex justify-end" v-model:current-page="taskTable.query.page"
            v-model:page-size="taskTable.query.pageSize" :total="taskTable.total" layout="total, prev, pager, next"
            @current-change="loadTasks" />
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
