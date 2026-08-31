<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getMonitorOverview } from '@/api/monitor.js'

/**
 * 监控中心：JVM运行信息 + 接口调用统计（10秒自动刷新）
 */
const overview = ref(null)
const loading = ref(false)
let timer = null

const load = () => {
    loading.value = true
    getMonitorOverview()
        .then((res) => {
            if (res?.code === 200) {
                overview.value = res.data
            }
        })
        .finally(() => {
            loading.value = false
        })
}

const formatUptime = (ms) => {
    if (!ms) return '-'
    const minutes = Math.floor(ms / 60000)
    const days = Math.floor(minutes / 1440)
    const hours = Math.floor((minutes % 1440) / 60)
    const mins = minutes % 60
    return days > 0 ? `${days}天${hours}时${mins}分` : `${hours}时${mins}分`
}

const errorRate = (row) => {
    if (!row.count) return '0%'
    return ((row.errors / row.count) * 100).toFixed(1) + '%'
}

onMounted(() => {
    load()
    timer = setInterval(load, 10000)
})
onBeforeUnmount(() => {
    if (timer) clearInterval(timer)
})
</script>

<template>
    <div class="page">
        <div class="flex justify-between items-center mb-10px">
            <span>接口监控（每10秒自动刷新）</span>
            <el-button type="primary" :loading="loading" @click="load">立即刷新</el-button>
        </div>

        <div class="cards">
            <el-card shadow="hover"><div class="card-value">{{ formatUptime(overview?.uptimeMs) }}</div><div class="card-label">运行时长</div></el-card>
            <el-card shadow="hover"><div class="card-value">{{ overview?.onlineCount ?? '-' }}</div><div class="card-label">当前在线</div></el-card>
            <el-card shadow="hover"><div class="card-value">{{ overview?.totalRequests ?? '-' }}</div><div class="card-label">接口总调用</div></el-card>
            <el-card shadow="hover">
                <div class="card-value" :style="{ color: overview?.totalErrors ? '#f53f3f' : '#00b578' }">{{ overview?.totalErrors ?? '-' }}</div>
                <div class="card-label">错误数(4xx/5xx)</div>
            </el-card>
            <el-card shadow="hover">
                <div class="card-value">{{ overview ? `${overview.heapUsedMb}/${overview.heapMaxMb}MB` : '-' }}</div>
                <div class="card-label">堆内存</div>
            </el-card>
            <el-card shadow="hover"><div class="card-value">{{ overview?.threadCount ?? '-' }}</div><div class="card-label">线程数</div></el-card>
            <el-card shadow="hover"><div class="card-value">{{ overview?.processors ?? '-' }}</div><div class="card-label">CPU核心</div></el-card>
            <el-card shadow="hover"><div class="card-value">{{ overview?.javaVersion ?? '-' }}</div><div class="card-label">Java版本</div></el-card>
        </div>

        <el-card shadow="never" class="mt-16px">
            <template #header>接口调用排行（按次 Top 50）</template>
            <el-table :data="overview?.apis || []" border size="small" :max-height="480">
                <el-table-column type="index" label="#" width="50" align="center" />
                <el-table-column prop="api" label="接口" min-width="280" show-overflow-tooltip />
                <el-table-column prop="count" label="调用次数" width="100" align="center" sortable />
                <el-table-column prop="avgMs" label="平均耗时(ms)" width="120" align="center" sortable />
                <el-table-column prop="maxMs" label="最大耗时(ms)" width="120" align="center" sortable />
                <el-table-column prop="errors" label="错误数" width="90" align="center" />
                <el-table-column label="错误率" width="90" align="center">
                    <template #default="scope">
                        <span :style="{ color: scope.row.errors ? '#f53f3f' : '' }">{{ errorRate(scope.row) }}</span>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}

.cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 14px;
}

.cards :deep(.el-card__body) {
    padding: 14px;
    text-align: center;
}

.card-value {
    font-size: 20px;
    font-weight: 700;
}

.card-label {
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
}
</style>
