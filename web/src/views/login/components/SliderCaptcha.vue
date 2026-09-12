<script setup>
import { onUnmounted, reactive, ref } from 'vue'

/**
 * 滑块拼图验证码：渲染背景/拼图块，拖到底部滑块对准缺口后emit最终X距离
 * （后端容差±5px校验）。Pointer事件同时支持鼠标与触屏
 */
const props = defineProps({
    bgImage: { type: String, required: true },
    pieceImage: { type: String, required: true },
    pieceY: { type: Number, default: 0 },
    width: { type: Number, default: 310 },
})
const emit = defineEmits(['dropped'])

const pieceX = ref(0)
const dragging = ref(false)
const drag = reactive({ startX: 0, current: 0 })
const trackRef = ref()

const onPointerDown = (e) => {
    dragging.value = true
    drag.startX = e.clientX
    drag.current = pieceX.value
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)
}
const onPointerMove = (e) => {
    if (!dragging.value) return
    const max = props.width - 44
    pieceX.value = Math.min(max, Math.max(0, drag.current + (e.clientX - drag.startX)))
}
const onPointerUp = () => {
    if (!dragging.value) return
    dragging.value = false
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', onPointerUp)
    emit('dropped', Math.round(pieceX.value))
}
onUnmounted(() => {
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', onPointerUp)
})
</script>

<template>
    <div class="slider-captcha">
        <div class="canvas" :style="{ width: width + 'px' }">
            <img class="bg" :src="bgImage" alt="滑块验证码背景" draggable="false" />
            <img class="piece" :src="pieceImage" alt="" draggable="false"
                :style="{ top: pieceY + 'px', left: pieceX + 'px' }" />
        </div>
        <div ref="trackRef" class="track">
            <div class="fill" :style="{ width: (pieceX / (width - 44)) * 100 + '%' }"></div>
            <div class="btn" @pointerdown.prevent="onPointerDown">
                <span :class="dragging ? '' : 'hint'">{{ dragging ? '　' : '»' }}</span>
            </div>
            <span class="tip" v-if="pieceX === 0">按住滑块拖动，对齐缺口</span>
        </div>
    </div>
</template>

<style scoped>
.slider-captcha {
    width: 100%;
}

.canvas {
    position: relative;
    border-radius: 6px;
    overflow: hidden;
}

.bg {
    display: block;
    width: 100%;
    user-select: none;
}

.piece {
    position: absolute;
    border-radius: 50%;
    box-shadow: 0 0 6px rgba(0, 0, 0, 0.4);
    user-select: none;
    pointer-events: none;
}

.track {
    position: relative;
    margin-top: 8px;
    height: 38px;
    background: #f0f2f5;
    border-radius: 19px;
    overflow: hidden;
}

.fill {
    height: 100%;
    background: #d9e4ff;
}

.btn {
    position: absolute;
    top: 0;
    left: 0;
    width: 44px;
    height: 38px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #2e5cf6;
    color: #fff;
    font-weight: 700;
    font-size: 18px;
    cursor: grab;
    user-select: none;
    touch-action: none;
}

.btn:active {
    cursor: grabbing;
}

.hint {
    letter-spacing: -2px;
}

.tip {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 12px;
    color: #909399;
    pointer-events: none;
}
</style>
