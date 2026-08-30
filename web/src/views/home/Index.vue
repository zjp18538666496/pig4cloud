<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/stats.js'

/**
 * 首页仪表盘：统计卡片 + 近7日登录趋势 + 最新公告
 */
const stats = ref(null)

const permissions = computed(() => {
    try {
        return JSON.parse(localStorage.getItem('userinfo'))?.permissions || []
    } catch {
        return []
    }
})
const isSuper = computed(() => permissions.value.includes('super'))

const statCards = computed(() => {
    if (!stats.value) {
        return []
    }
    const cards = [
        { label: '用户数', value: stats.value.userCount, color: '#2e5cf6' },
        { label: '角色数', value: stats.value.roleCount, color: '#00b578' },
        { label: '部门数', value: stats.value.deptCount, color: '#ff8f1f' },
    ]
    if (isSuper.value) {
        cards.push({ label: '租户数', value: stats.value.tenantCount, color: '#8f4bff' })
    }
    cards.push({ label: '今日登录', value: stats.value.todayLoginCount, color: '#f53f3f' })
    cards.push({ label: '当前在线', value: stats.value.onlineCount, color: '#14c9c9' })
    return cards
})

/**
 * 登录趋势图
 */
const chartRef = ref()
let chart = null

const renderChart = () => {
    if (!chartRef.value || !stats.value) {
        return
    }
    if (!chart) {
        chart = echarts.init(chartRef.value)
    }
    const trend = stats.value.loginTrend || []
    chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 30, bottom: 30 },
        xAxis: {
            type: 'category',
            data: trend.map((item) => item.date),
            boundaryGap: false,
        },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
            {
                name: '登录次数',
                type: 'line',
                smooth: true,
                areaStyle: { opacity: 0.15 },
                itemStyle: { color: '#2e5cf6' },
                data: trend.map((item) => item.count),
            },
        ],
    })
}

const handleResize = () => chart && chart.resize()

const loadStats = () => {
    getDashboardStats().then((res) => {
        if (res?.code === 200) {
            stats.value = res.data
            renderChart()
        }
    })
}

onMounted(() => {
    loadStats()
    window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    if (chart) {
        chart.dispose()
        chart = null
    }
})
</script>

<template>
    <div class="dashboard">
        <!-- 欢迎语 -->
        <div class="welcome">
            <span class="title">PIGX ADMIN</span>
            <span class="sub">统一的RBAC权限管理平台，登录后即可开始管理</span>
        </div>

        <!-- 统计卡片 -->
        <div class="cards">
            <el-card v-for="card in statCards" :key="card.label" shadow="hover" class="card">
                <div class="card-value" :style="{ color: card.color }">{{ card.value ?? '-' }}</div>
                <div class="card-label">{{ card.label }}</div>
            </el-card>
        </div>

        <div class="bottom">
            <!-- 登录趋势 -->
            <el-card shadow="never" class="chart-card">
                <template #header>近7日登录趋势</template>
                <div ref="chartRef" class="chart"></div>
            </el-card>
            <!-- 最新公告 -->
            <el-card shadow="never" class="notice-card">
                <template #header>最新公告</template>
                <el-empty v-if="!stats?.latestNotices?.length" description="暂无公告" :image-size="60" />
                <ul v-else class="notice-list">
                    <li v-for="notice in stats.latestNotices" :key="notice.id" class="notice-item">
                        <span class="notice-title">{{ notice.title }}</span>
                        <span class="notice-time">{{ notice.create_time }}</span>
                    </li>
                </ul>
            </el-card>
        </div>
    </div>
</template>

<style scoped>
.dashboard {
    padding: 20px;
}

.welcome {
    margin-bottom: 20px;
    display: flex;
    align-items: baseline;
    gap: 12px;
}

.welcome .title {
    font-size: 20px;
    font-weight: 700;
    color: #2e5cf6;
}

.welcome .sub {
    font-size: 13px;
    color: #909399;
}

.cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 16px;
}

.card :deep(.el-card__body) {
    padding: 16px;
    text-align: center;
}

.card-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.3;
}

.card-label {
    margin-top: 4px;
    color: #909399;
    font-size: 13px;
}

.bottom {
    display: flex;
    gap: 16px;
    margin-top: 16px;
    align-items: stretch;
}

.chart-card {
    flex: 1;
}

.notice-card {
    width: 360px;
}

.chart {
    width: 100%;
    height: 300px;
}

.notice-list {
    list-style: none;
    margin: 0;
    padding: 0;
}

.notice-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px dashed #e4e7ed;
}

.notice-item:last-child {
    border-bottom: none;
}

.notice-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.notice-time {
    color: #909399;
    font-size: 12px;
    white-space: nowrap;
}
</style>
