<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getTenantReport, getScreenSummary } from '@/api/stats.js'
import { applyChartTheme, onThemeChange } from '@/utils/chartTheme.js'

/**
 * 数据大屏：总览卡片 + 登录趋势 + 审批量 + 通知送达率 + 租户维度报表（深色模式自适应）
 */
const report = ref(null)
const loading = ref(false)
const summary = reactive({ loginTrend: [], todayLoginCount: 0, onlineCount: 0, userCount: 0, tenantCount: 0, approval: {}, notify: {} })
let barChart = null
let trendChart = null
let pieChart = null
const barRef = ref()
const trendRef = ref()
const pieRef = ref()
let stopThemeWatch = null

const load = () => {
    loading.value = true
    Promise.all([getTenantReport(), getScreenSummary()])
        .then(([reportRes, summaryRes]) => {
            if (reportRes?.code === 200) report.value = reportRes.data
            if (summaryRes?.code === 200) Object.assign(summary, summaryRes.data)
            renderAll()
        })
        .finally(() => {
            loading.value = false
        })
}

const renderAll = () => {
    renderBar()
    renderTrend()
    renderPie()
}

const renderBar = () => {
    if (!barRef.value || !report.value) return
    if (!barChart) {
        barChart = echarts.init(barRef.value)
    }
    const rows = report.value
    const option = applyChartTheme({
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
    barChart.setOption(option, true)
}

const renderTrend = () => {
    if (!trendRef.value) return
    if (!trendChart) {
        trendChart = echarts.init(trendRef.value)
    }
    const trend = summary.loginTrend || []
    const option = applyChartTheme({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', boundaryGap: false, data: trend.map(t => t.date) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
            name: '登录次数', type: 'line', smooth: true,
            areaStyle: { opacity: 0.15 }, itemStyle: { color: '#2e5cf6' },
            data: trend.map(t => t.count),
        }],
    })
    trendChart.setOption(option, true)
}

const renderPie = () => {
    if (!pieRef.value) return
    if (!pieChart) {
        pieChart = echarts.init(pieRef.value)
    }
    const a = summary.approval || {}
    const option = applyChartTheme({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [{
            type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'],
            label: { show: false },
            data: [
                { name: '待审批', value: a.pending || 0, itemStyle: { color: '#ff8f1f' } },
                { name: '已通过', value: a.approved || 0, itemStyle: { color: '#00b578' } },
                { name: '已驳回', value: a.rejected || 0, itemStyle: { color: '#f53f3f' } },
            ],
        }],
    })
    pieChart.setOption(option, true)
}

const quotaColor = (row) => {
    if (row.quotaUsedPercent == null) return ''
    if (row.quotaUsedPercent >= 90) return '#f53f3f'
    if (row.quotaUsedPercent >= 70) return '#ff8f1f'
    return '#00b578'
}

const handleResize = () => {
    barChart?.resize()
    trendChart?.resize()
    pieChart?.resize()
}

onMounted(() => {
    load()
    window.addEventListener('resize', handleResize)
    // 暗黑模式切换时重渲染全部图表
    stopThemeWatch = onThemeChange(() => renderAll())
})
onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    stopThemeWatch?.()
    for (const chart of [barChart, trendChart, pieChart]) {
        chart?.dispose()
    }
    barChart = trendChart = pieChart = null
})
</script>

<template>
    <div class="page">
        <div class="flex justify-between items-center mb-10px">
            <span>数据大屏 · 租户使用报表</span>
            <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
        </div>

        <!-- 总览卡片 -->
        <div class="summary-cards mb-14px">
            <el-card shadow="never"><div class="s-label">今日登录</div><div class="s-value">{{ summary.todayLoginCount }}</div></el-card>
            <el-card shadow="never"><div class="s-label">当前在线</div><div class="s-value" style="color:#14c9c9">{{ summary.onlineCount }}</div></el-card>
            <el-card shadow="never"><div class="s-label">用户总数</div><div class="s-value">{{ summary.userCount }}</div></el-card>
            <el-card shadow="never"><div class="s-label">租户数</div><div class="s-value">{{ summary.tenantCount }}</div></el-card>
            <el-card shadow="never"><div class="s-label">审批待办</div><div class="s-value" style="color:#ff8f1f">{{ (summary.approval || {}).pending || 0 }}</div></el-card>
            <el-card shadow="never"><div class="s-label">通知送达率</div><div class="s-value" style="color:#00b578">{{ (summary.notify || {}).successRate || '-' }}</div></el-card>
        </div>

        <el-row :gutter="14" class="mb-14px">
            <el-col :xs="24" :md="14">
                <el-card shadow="never"><template #header>近7日登录趋势</template><div ref="trendRef" class="chart chart-sm"></div></el-card>
            </el-col>
            <el-col :xs="24" :md="10">
                <el-card shadow="never"><template #header>审批量分布</template><div ref="pieRef" class="chart chart-sm"></div></el-card>
            </el-col>
        </el-row>

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

.chart-sm {
    height: 260px;
}

.summary-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
}

.summary-cards .s-label {
    font-size: 12px;
    color: #909399;
}

.summary-cards .s-value {
    font-size: 24px;
    font-weight: 700;
    margin-top: 4px;
}

@media (max-width: 768px) {
    .page {
        padding: 10px;
    }

    .chart {
        height: 260px;
    }
}
</style>
