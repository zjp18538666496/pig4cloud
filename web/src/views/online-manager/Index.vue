<script setup>
import { onMounted, onUnmounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOnlineUsers, kickOut } from '@/api/online.js'
import { debounce } from '@/utils/utils.js'

/**
 * 在线用户管理：会话列表 + 强制下线
 */
const onlineTable = reactive({
    query: { username: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getOnlineList = () => {
    getOnlineUsers(onlineTable.query).then((res) => {
        if (res?.code === 200) {
            onlineTable.rows = res.data.rows
            onlineTable.total = res.data.total
        } else {
            ElMessage.error(`获取在线用户失败！${res?.message}`)
        }
    })
}
getOnlineList()

const handleSearch = () => {
    onlineTable.query.page = 1
    getOnlineList()
}

const handleReset = () => {
    onlineTable.query.username = ''
    handleSearch()
}

const handleSizeChange = () => {
    onlineTable.query.page = 1
    getOnlineList()
}

/**
 * 强制下线
 */
const handleKick = (row) => {
    ElMessageBox.confirm(`您确定将用户【${row.username}】强制下线吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        kickOut({ tokenJti: row.tokenJti }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '已强制下线')
                getOnlineList()
            } else {
                ElMessage.error(`操作失败！${res?.message}`)
            }
        })
    })
}

const updateTableHeight = () => {
    onlineTable.height = window.innerHeight - 50 - 30 - 40 - 52 - 52
}
onMounted(() => {
    window.addEventListener('resize', debounce(updateTableHeight, 500))
})
onUnmounted(() => {
    window.removeEventListener('resize', debounce(updateTableHeight, 500))
})
</script>

<template>
    <div class="page">
        <div class="flex justify-between mb-10px">
            <div class="flex items-center gap-10px">
                <span>操作人(模糊)：</span>
                <el-input class="w-240px" v-model="onlineTable.query.username" placeholder="请输入用户名" clearable @keyup.enter="handleSearch" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" @click="getOnlineList">刷新</el-button>
        </div>
        <el-table :data="onlineTable.rows" border :max-height="onlineTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="username" label="账号" width="130" />
            <el-table-column prop="name" label="昵称" width="130" />
            <el-table-column prop="tenantId" label="租户ID" width="90" align="center" />
            <el-table-column prop="ip" label="登录IP" width="150" />
            <el-table-column prop="region" label="归属地" width="150" show-overflow-tooltip />
            <el-table-column prop="browser" label="浏览器" width="130" />
            <el-table-column prop="loginTime" label="登录时间" width="170" />
            <el-table-column prop="lastAccessTime" label="最后访问时间" width="170" />
            <el-table-column label="操作" width="110" align="center">
                <template #default="scope">
                    <el-button size="small" type="danger" link v-permission="['online:kick']" @click="handleKick(scope.row)">强制下线</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="onlineTable.query.page"
            v-model:page-size="onlineTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="onlineTable.total"
            @size-change="handleSizeChange"
            @current-change="getOnlineList"
        />
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
