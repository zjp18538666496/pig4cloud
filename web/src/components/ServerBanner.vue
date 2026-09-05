<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

/**
 * 全局断连横幅：后端不可达时顶部提示（去重，恢复后自动消失），替代逐条报错弹窗
 */
const visible = ref(false)
let errCount = 0
const onServerError = () => {
    errCount++
    visible.value = true
}
const onServerOk = () => {
    errCount = 0
    visible.value = false
}
onMounted(() => {
    window.addEventListener('pigx:server-error', onServerError)
    window.addEventListener('pigx:server-ok', onServerOk)
})
onUnmounted(() => {
    window.removeEventListener('pigx:server-error', onServerError)
    window.removeEventListener('pigx:server-ok', onServerOk)
})

const retry = () => location.reload()
</script>

<template>
    <transition name="fade">
        <div v-if="visible" class="server-banner">
            ⚠ 后端服务连接失败，页面数据无法加载。请确认服务已启动，
            <span class="retry" @click="retry">点击重试</span>
        </div>
    </transition>
</template>

<style scoped>
.server-banner {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 3000;
    padding: 8px 16px;
    text-align: center;
    font-size: 13px;
    color: #fff;
    background-color: #f56c6c;
}

.retry {
    font-weight: 700;
    text-decoration: underline;
    cursor: pointer;
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
