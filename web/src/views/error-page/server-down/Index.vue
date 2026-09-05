<script setup>
import { ref } from 'vue'

/**
 * 后端服务不可用兜底页：白屏替代方案，可一键重试
 */
const retrying = ref(false)
const retry = () => {
    retrying.value = true
    location.reload()
}
</script>

<template>
    <div class="server-down">
        <div class="box">
            <div class="icon">⚠</div>
            <div class="title">服务暂时不可用</div>
            <div class="desc">无法连接后端服务，请确认服务已启动或联系管理员。</div>
            <el-button type="primary" :loading="retrying" @click="retry">重试</el-button>
        </div>
    </div>
</template>

<style scoped>
.server-down {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100vw;
    height: 100vh;
    background-color: #f5f7fa;
}

.box {
    text-align: center;
}

.icon {
    font-size: 56px;
}

.title {
    margin: 16px 0 8px;
    font-size: 20px;
    font-weight: 700;
    color: #303133;
}

.desc {
    margin-bottom: 24px;
    font-size: 13px;
    color: #909399;
}
</style>
