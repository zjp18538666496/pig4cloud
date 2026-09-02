<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getTenantReport } from '@/api/stats.js'

/**
 * 数据大屏：租户维度使用报表（用户数/角色数/在线/配额使用率/30天登录趋势）
 */
const report = ref(null)
const loading = ref(false)
let barChart = null
const barRef = ref()

const load = () => {
    loading.value = true
    getTenantReport()
        .then((res) => {
            if (res?.code === 200) {
                report.value = res.data
                renderBar()
            }
        })
        .finally(() => {
            loading.value = false
        })
}

const renderBar = () => {
    if (!barRef.value || !report.value) return
    if (!barChart) {
        barChart = echarts.init(barRef.value)
    }
    const rows = report.value
    barChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['用户数', '角色数', '近30天登录'] },
        grid: { left: 50, right: 20, top: 40, bottom: 40 },
        xAxis: { type: 'category', data: rows.map(r => r.tenantName) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
            { name: '用户数', type: 'bar', itemStyle: { color: '#2e5cf6' }, data: rows.map(r => r.userCount) },
            { name: '角色数', type: 'bar', itemStyle: { color: '#00b578' }, data: rows.map(r => r.roleCount) },
            { name: '近30天登录', type: 'bar', itemStyle: { color: '#ff8f1f' }, data: rows.map(r => r.login30d) },
        ],
    })
}

const quotaColor = (row) => {
    if (row.quotaUsedPercent == null) return ''
    if (row.quotaUsedPercent >= 90) return '#f53f3f'
    if (row.quotaUsedPercent >= 70) return '#ff8f1f'
    return '#00b578'
}

const handleResize = () => barChart && barChart.resize()

onMounted(() => {
    load()
    window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    if (barChart) {
        barChart.dispose()
        barChart = null
    }
})
</script>

<template>
    <div class="page">
        <div class="flex justify-between items-center mb-10px">
            <span>数据大屏 · 租户使用报表</span>
            <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
        </div>

        <!-- 租户大卡片 -->
        <div class="tenant-cards">
            <el-card v-for="row in report || []" :key="row.tenantId" shadow="hover" class="tenant-card">
                <div class="flex justify-between items-center">
                    <span class="tenant-name">{{ row.tenantName }}</span>
                    <el-tag :type="row.status === '1' ? 'success' : 'danger'" size="small">
                        {{ row.status === '1' ? '启用' : '禁用' }}
                    </el-tag>
                </div>
                <div class="tenant-nums">
                    <div class="num"><div class="v">{{ row.userCount }}</div><div class="l">用户</div></div>
                    <div class="num"><div class="v">{{ row.roleCount }}</div><div class="l">角色</div></div>
                    <div class="num"><div class="v">{{ row.deptCount }}</div><div class="l">部门</div></div>
                    <div class="num"><div class="v" style="color:#14c9c9">{{ row.onlineCount }}</div><div class="l">在线</div></div>
                    <div class="num"><div class="v" style="color:#ff8f1f">{{ row.login30d }}</div><div class="l">30天登录</div></div>
                </div>
                <div class="mt-8px" v-if="row.userLimit">
                    <div class="flex justify-between text-12px color-#909399">
                        <span>用户配额</span><span>{{ row.userCount }}/{{ row.userLimit }}</span>
                    </div>
                    <el-progress :percentage="Math.min(100, row.quotaUsedPercent || 0)" :color="quotaColor(row)" :stroke-width="8" />
                </div>
                <div class="mt-8px text-12px color-#909399" v-else>用户数不限</div>
            </el-card>
        </div>

        <el-card shadow="never" class="mt-16px">
            <template #header>各租户规模与活跃度</template>
            <div ref="barRef" class="chart"></div>
        </el-card>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}

.tenant-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 14px;
}

.tenant-name {
    font-weight: 700;
    font-size: 15px;
}

.tenant-nums {
    display: flex;
    justify-content: space-between;
    margin-top: 12px;
}

.num .v {
    font-size: 22px;
    font-weight: 700;
    text-align: center;
    color: #2e5cf6;
}

.num .l {
    font-size: 12px;
    color: #909399;
    text-align: center;
}

.chart {
    width: 100%;
    height: 340px;
}
</style>
