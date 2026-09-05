<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import service from '@/utils/request.js'

/**
 * 缓存监控（仅平台超管）：查看StateStore缓存（redis/内存）——分组统计、键明细、删除
 */
const overview = reactive({ mode: '-', totalKeys: 0, dbSize: null, usedMemoryHuman: null, groups: [] })
const keyTable = reactive({ group: '', keyword: '', page: 1, pageSize: 20, rows: [], total: 0, loading: false })
const loading = ref(false)

const loadOverview = () => {
    service({ url: '/monitor/cache/overview', method: 'get' }).then((res) => {
        if (res?.code === 200) Object.assign(overview, res.data)
    })
}
const loadKeys = () => {
    keyTable.loading = true
    service({
        url: '/monitor/cache/keys', method: 'get',
        params: { group: keyTable.group, keyword: keyTable.keyword, page: keyTable.page, pageSize: keyTable.pageSize },
    }).then((res) => {
        if (res?.code === 200) { keyTable.rows = res.data.rows; keyTable.total = res.data.total }
    }).finally(() => { keyTable.loading = false })
}
const refreshAll = () => { loadOverview(); keyTable.page = 1; loadKeys() }
onMounted(refreshAll)

const pickGroup = (g) => {
    keyTable.group = keyTable.group === g ? '' : g
    keyTable.page = 1
    loadKeys()
}
const fmtTtl = (ttl) => {
    if (ttl === null || ttl === undefined) return '-'
    if (ttl < 0) return '永不过期'
    if (ttl < 60) return ttl + '秒'
    if (ttl < 3600) return Math.floor(ttl / 60) + '分钟'
    return Math.floor(ttl / 3600) + '小时'
}
const handleDelete = (row) => {
    ElMessageBox.confirm(`删除缓存键【${row.key}】？相关会话/验证码/锁将立即失效。`, '危险操作', {
        confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
    }).then(() => {
        service({ url: '/monitor/cache/delete', method: 'post', data: { key: row.key } }).then((res) => {
            if (res?.code === 200) { ElMessage.success('已删除'); loadOverview(); loadKeys() } else ElMessage.error(res?.message)
        })
    }).catch(() => { })
}
</script>

<template>
    <div class="page">
        <el-row :gutter="12" class="mb-10px">
            <el-col :span="6"><el-card shadow="never"><div class="stat-label">缓存模式</div><div class="stat-value">{{ overview.mode === 'redis' ? 'Redis' : '内存（单机）' }}</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-label">StateStore键总数</div><div class="stat-value">{{ overview.totalKeys }}</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-label">Redis内存占用</div><div class="stat-value">{{ overview.usedMemoryHuman || '-' }}</div></el-card></el-col>
            <el-col :span="6"><el-card shadow="never"><div class="stat-label">Redis键总数(dbSize)</div><div class="stat-value">{{ overview.dbSize ?? '-' }}</div></el-card></el-col>
        </el-row>

        <el-row :gutter="12">
            <el-col :span="7">
                <el-card shadow="never">
                    <template #header>
                        <div class="flex justify-between items-center">
                            <span>按前缀分组</span>
                            <el-button size="small" @click="refreshAll">刷新</el-button>
                        </div>
                    </template>
                    <el-table :data="overview.groups" size="small" highlight-current-row @row-click="pickGroup" class="pointer-row">
                        <el-table-column prop="group" label="前缀分组" min-width="120" />
                        <el-table-column prop="count" label="键数量" width="80" align="center" />
                    </el-table>
                    <div class="mt-8px text-12px" style="color:#909399">点击分组可筛选右侧键列表，再次点击取消</div>
                </el-card>
            </el-col>
            <el-col :span="17">
                <el-card shadow="never">
                    <template #header>
                        <div class="flex justify-between items-center">
                            <div class="flex items-center gap-10px">
                                <el-input class="w-180px" v-model="keyTable.keyword" placeholder="键名包含…" clearable @keyup.enter="keyTable.page = 1; loadKeys()" />
                                <el-button @click="keyTable.page = 1; loadKeys">查询</el-button>
                                <el-tag v-if="keyTable.group" closable @close="keyTable.group = ''; keyTable.page = 1; loadKeys()">{{ keyTable.group }}</el-tag>
                            </div>
                            <span class="text-12px" style="color:#909399">共 {{ keyTable.total }} 个键</span>
                        </div>
                    </template>
                    <el-table :data="keyTable.rows" border size="small" v-loading="keyTable.loading">
                        <el-table-column prop="key" label="键" min-width="240" show-overflow-tooltip />
                        <el-table-column label="剩余TTL" width="90" align="center">
                            <template #default="scope">{{ fmtTtl(scope.row.ttlSeconds) }}</template>
                        </el-table-column>
                        <el-table-column prop="valuePreview" label="值预览" min-width="300" show-overflow-tooltip />
                        <el-table-column label="操作" width="80" align="center">
                            <template #default="scope">
                                <el-button size="small" type="danger" link @click="handleDelete(scope.row)">删除</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-pagination class="mt-10px flex justify-end" v-model:current-page="keyTable.page"
                        v-model:page-size="keyTable.pageSize" :page-sizes="[10, 20, 50, 100]" :total="keyTable.total"
                        layout="total, sizes, prev, pager, next" @current-change="loadKeys" @size-change="keyTable.page = 1; loadKeys()" />
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<style scoped>
.page {
    padding: 15px;
}

.stat-label {
    font-size: 12px;
    color: #909399;
}

.stat-value {
    font-size: 22px;
    font-weight: 600;
    margin-top: 4px;
}

.pointer-row {
    cursor: pointer;
}
</style>
