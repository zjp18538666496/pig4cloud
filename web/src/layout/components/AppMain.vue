<script setup></script>

<template>
    <div class="main-container">
        <!-- keep-alive按完整路径缓存组件实例，配合多标签页保留页面状态（LRU上限10个） -->
        <router-view v-slot="{ Component, route }">
            <keep-alive :max="10">
                <component :is="Component" :key="route.fullPath" />
            </keep-alive>
        </router-view>
    </div>
</template>

<style scoped>
.main-container {
    padding: 20px;
    width: 100%;
    height: calc(100vh - 80px);
    overflow: auto;
    border-radius: 8px;
    background-color: #fff;
}
</style>
