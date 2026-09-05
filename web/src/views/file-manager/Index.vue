<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import service from '@/utils/request.js'

/**
 * 文件管理（仅平台超管）：查看当前存储(local/s3)里的全部文件并删除；ftp不支持列表
 */
const table = reactive({ prefix: '', rows: [], loading: false })
const storageType = ref('-')

const loadFiles = () => {
    table.loading = true
    service({ url: '/file/list', method: 'get', params: { prefix: table.prefix } }).then((res) => {
        if (res?.code === 200) table.rows = res.data
        else ElMessage.error(res?.message)
    }).finally(() => { table.loading = false })
}
onMounted(loadFiles)

const fmtSize = (bytes) => {
    if (bytes === null || bytes === undefined) return '-'
    if (bytes < 1024) return bytes + 'B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
    return (bytes / 1024 / 1024).toFixed(2) + 'MB'
}
const fmtTime = (ms) => (ms ? new Date(ms).toLocaleString('zh-CN', { hour12: false }) : '-')

const handleDelete = (row) => {
    ElMessageBox.confirm(`删除文件【${row.path}】？删除后相关功能（如头像/导出下载）将不可用。`, '危险操作', {
        confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
    }).then(() => {
        service({ url: '/file/delete', method: 'post', data: { path: row.path } }).then((res) => {
            if (res?.code === 200) { ElMessage.success('已删除'); loadFiles() } else ElMessage.error(res?.message)
        })
    }).catch(() => { })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between items-center mb-10px">
            <div class="flex items-center gap-10px">
                <el-input class="w-240px" v-model="table.prefix" placeholder="路径包含…" clearable @keyup.enter="loadFiles" />
                <el-button type="primary" @click="loadFiles">查询</el-button>
            </div>
            <span class="text-12px" style="color:#909399">共 {{ table.rows.length }} 个文件；仅平台超管可访问</span>
        </div>
        <el-table :data="table.rows" border v-loading="table.loading">
            <el-table-column prop="path" label="文件路径" min-width="300" show-overflow-tooltip />
            <el-table-column label="大小" width="110" align="center">
                <template #default="scope">{{ fmtSize(scope.row.sizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="最后修改" width="180" align="center">
                <template #default="scope">{{ fmtTime(scope.row.lastModifiedMs) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
                <template #default="scope">
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
