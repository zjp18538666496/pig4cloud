<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { globalSearch } from '@/api/search.js'
import { Search } from '@element-plus/icons-vue'

/**
 * 顶栏全局搜索：模糊匹配菜单/用户/角色/租户，Ctrl+K聚焦，点击结果跳转
 */
const router = useRouter()
const keyword = ref('')
const visible = ref(false)
const loading = ref(false)
const result = reactive({ menus: [], users: [], roles: [], tenants: [] })
let searchTimer = null

const doSearch = () => {
    const kw = keyword.value.trim()
    if (!kw) {
        visible.value = false
        return
    }
    loading.value = true
    globalSearch(kw)
        .then((res) => {
            if (res?.code === 200) {
                Object.assign(result, res.data)
                visible.value = true
            }
        })
        .finally(() => {
            loading.value = false
        })
}

const onInput = () => {
    clearTimeout(searchTimer)
    searchTimer = setTimeout(doSearch, 300)
}

const goMenu = (route) => {
    if (route) {
        router.push(route)
    }
    visible.value = false
    keyword.value = ''
}

const goUserManager = () => goMenu('/user-manager')
const goRoleManager = () => goMenu('/role-manager')
const goTenantManager = () => goMenu('/tenant-manager')

const onFocus = () => {
    if (keyword.value.trim()) {
        visible.value = true
    }
}

const onGlobalKeydown = (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        document.getElementById('global-search-input')?.focus()
    }
}

const hidePanel = () => {
    // 延迟隐藏，让结果项的click先触发
    setTimeout(() => (visible.value = false), 200)
}

onMounted(() => {
    window.addEventListener('keydown', onGlobalKeydown)
})
onUnmounted(() => {
    window.removeEventListener('keydown', onGlobalKeydown)
})
</script>

<template>
    <div class="global-search">
        <el-input
            id="global-search-input"
            v-model="keyword"
            :prefix-icon="Search"
            placeholder="搜索菜单/用户/角色/租户 (Ctrl+K)"
            clearable
            class="search-input"
            @input="onInput"
            @focus="onFocus"
            @blur="hidePanel"
        />
        <div v-if="visible && keyword" class="search-panel">
            <div v-if="loading" class="panel-tip">搜索中...</div>
            <template v-else>
                <div v-if="!result.menus.length && !result.users.length && !result.roles.length && !result.tenants.length" class="panel-tip">
                    无匹配结果
                </div>
                <div v-if="result.menus.length" class="panel-group">
                    <div class="group-title">菜单</div>
                    <div v-for="menu in result.menus" :key="menu.route" class="panel-item" @mousedown="goMenu(menu.route)">
                        {{ menu.name }}
                        <span class="item-path">{{ menu.route }}</span>
                    </div>
                </div>
                <div v-if="result.users.length" class="panel-group">
                    <div class="group-title">用户</div>
                    <div v-for="user in result.users" :key="user.username" class="panel-item" @mousedown="goUserManager">
                        {{ user.name }}（{{ user.username }}）
                    </div>
                </div>
                <div v-if="result.roles.length" class="panel-group">
                    <div class="group-title">角色</div>
                    <div v-for="role in result.roles" :key="role.roleCode" class="panel-item" @mousedown="goRoleManager">
                        {{ role.roleName }}（{{ role.roleCode }}）
                    </div>
                </div>
                <div v-if="result.tenants.length" class="panel-group">
                    <div class="group-title">租户</div>
                    <div v-for="tenant in result.tenants" :key="tenant.tenantId" class="panel-item" @mousedown="goTenantManager">
                        {{ tenant.tenantName }}（{{ tenant.tenantId }}）
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<style scoped>
.global-search {
    position: relative;
    margin-right: 12px;
}

.search-input {
    width: 240px;
}

.search-panel {
    position: absolute;
    top: 42px;
    left: 0;
    width: 320px;
    max-height: 420px;
    overflow: auto;
    background: var(--el-bg-color-overlay, #fff);
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    z-index: 2100;
    padding: 6px 0;
}

.panel-tip {
    padding: 10px 14px;
    color: #909399;
    font-size: 13px;
}

.panel-group .group-title {
    padding: 6px 14px 2px;
    color: #909399;
    font-size: 12px;
}

.panel-item {
    padding: 8px 14px;
    cursor: pointer;
    font-size: 13px;
}

.panel-item:hover {
    background: rgba(46, 92, 246, 0.08);
}

.item-path {
    color: #909399;
    font-size: 12px;
    margin-left: 8px;
}

html.dark .search-panel {
    border-color: #363637;
}
</style>
