<script setup>
import { onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { selectMenuLists } from '@/api/menu.js'

/**
 * 多标签页：访问过的路由自动入栈，点击切换/关闭；keep-alive缓存由AppMain负责
 */
const route = useRoute()
const router = useRouter()

const tabs = reactive({
    list: [{ path: '/home', title: '首页', fixed: true }],
    active: '/home',
})

// 菜单路径 -> 名称 映射（标签标题用）
const titleMap = reactive({})
onMounted(() => {
    selectMenuLists({ menuType: '' }).then((res) => {
        if (res?.code === 200) {
            const flat = (nodes) => {
                for (const node of nodes || []) {
                    if (node.route) {
                        titleMap[node.route] = node.menu_name
                    }
                    flat(node.children)
                }
            }
            flat(res.data)
            syncCurrent()
        }
    })
})

const addTab = (path) => {
    if (!path || path === '/login' || path === '/404') {
        return
    }
    if (!tabs.list.some((tab) => tab.path === path)) {
        tabs.list.push({ path, title: titleMap[path] || path })
    }
    tabs.active = path
}

const syncCurrent = () => addTab(route.path)

watch(
    () => route.path,
    () => syncCurrent(),
)

const switchTab = (path) => {
    if (path !== route.path) {
        router.push(path)
    }
}

const closeTab = (path) => {
    const index = tabs.list.findIndex((tab) => tab.path === path)
    if (index < 0 || tabs.list[index].fixed) {
        return
    }
    tabs.list.splice(index, 1)
    // 关闭的是当前页：切到相邻标签
    if (path === route.path) {
        const next = tabs.list[index] || tabs.list[index - 1]
        router.push(next ? next.path : '/home')
    }
}
</script>

<template>
    <div class="tabs">
        <el-tag
            v-for="tab in tabs.list"
            :key="tab.path"
            class="tab-item"
            :effect="tab.path === tabs.active ? 'dark' : 'plain'"
            :closable="!tab.fixed"
            @click="switchTab(tab.path)"
            @close="closeTab(tab.path)"
        >
            {{ tab.title }}
        </el-tag>
    </div>
</template>

<style scoped>
.tabs {
    padding: 6px 12px 0;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    background: #f8f8f8;
}

.tab-item {
    cursor: pointer;
    user-select: none;
}
</style>
