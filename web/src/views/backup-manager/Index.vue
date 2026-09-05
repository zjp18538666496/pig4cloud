<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import service from '@/utils/request.js'

/**
 * 备份管理（仅平台超管）：纯JDBC全量备份（建表+数据INSERT），创建/下载/删除
 */
const table = reactive({ rows: [], loading: false })
const creating = ref(false)

const loadList = () => {
    table.loading = true
    service({ url: '/backup/list', method: 'get' }).then((res) => {
        if (res?.code === 200) table.rows = res.data
        else ElMessage.error(res?.message)
    }).finally(() => { table.loading = false })
}
onMounted(loadList)

const create = () => {
    ElMessageBox.confirm('立即创建数据库全量备份（建表语句+数据INSERT，小库秒级）？', '创建备份')
        .then(() => {
            creating.value = true
            service({ url: '/backup/create', method: 'post' }).then((res) => {
                if (res?.code === 200) { ElMessage.success('备份完成：' + res.data); loadList() } else ElMessage.error(res?.message)
            }).finally(() => { creating.value = false })
        }).catch(() => { })
}

const fmtSize = (bytes) => (bytes < 1024 ? bytes + 'B' : bytes < 1024 * 1024 ? (bytes / 1024).toFixed(1) + 'KB' : (bytes / 1024 / 1024).toFixed(2) + 'MB')
const fmtTime = (ms) => new Date(ms).toLocaleString('zh-CN', { hour12: false })

const handleDownload = (row) => {
    window.open('/api/backup/download/' + encodeURIComponent(row.name), '_blank')
}
const handleDelete = (row) => {
    ElMessageBox.confirm(`删除备份文件【${row.name}】？`, '提示', { type: 'warning' }).then(() => {
        service({ url: '/backup/delete', method: 'post', data: { name: row.name } }).then((res) => {
            if (res?.code === 200) { ElMessage.success('已删除'); loadList() } else ElMessage.error(res?.message)
        })
    }).catch(() => { })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between items-center mb-10px">
            <span class="text-12px" style="color:#909399">
                纯JDBC全量备份（建表语句+数据INSERT），不依赖mysqldump；文件在应用backup/目录，可配合系统级定时任务做每日备份
            </span>
            <el-button type="primary" :loading="creating" @click="create">立即备份</el-button>
        </div>
        <el-table :data="table.rows" border v-loading="table.loading">
            <el-table-column prop="name" label="备份文件" min-width="260" />
            <el-table-column label="大小" width="110" align="center">
                <template #default="scope">{{ fmtSize(scope.row.sizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="备份时间" width="180" align="center">
                <template #default="scope">{{ fmtTime(scope.row.lastModifiedMs) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link @click="handleDownload(scope.row)">下载</el-button>
                    <el-button size="small" type="danger" link @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
