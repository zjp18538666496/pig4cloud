<template>
    <div>
        <div class="mb-20px">
            操作人
            <el-input
                v-model="logTable.query.username"
                class="w240px"
                placeholder="操作人(模糊)"
                @keyup.enter="handleSearch"
            />
            <el-button class="ml-10px" @click="handleSearch">查询</el-button>
            <el-button type="primary" @click="handleReset">重置</el-button>
        </div>
        <el-table :data="logTable.rows" border style="width: 100%" :max-height="logTable.height">
            <el-table-column prop="createTime" label="时间" width="170" align="center" />
            <el-table-column prop="username" label="操作人" width="110" align="center" />
            <el-table-column prop="module" label="模块" width="100" align="center" />
            <el-table-column prop="operation" label="操作" width="100" align="center" />
            <el-table-column prop="method" label="方式" width="70" align="center" />
            <el-table-column prop="url" label="接口地址" align="left" show-overflow-tooltip />
            <el-table-column prop="params" label="请求参数" align="left" show-overflow-tooltip />
            <el-table-column prop="ip" label="来源IP" width="130" align="center" />
            <el-table-column prop="success" label="结果" width="70" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.success ? 'success' : 'danger'">
                        {{ scope.row.success ? '成功' : '失败' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="costMs" label="耗时(ms)" width="85" align="center" />
            <el-table-column prop="errorMsg" label="失败信息" align="left" show-overflow-tooltip />
        </el-table>
        <el-pagination
            v-model:current-page="logTable.query.page"
            v-model:page-size="logTable.query.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="logTable.total"
            layout="total, sizes, prev, pager, next, jumper"
            class="mt-10px justify-end"
            @current-change="getLogList"
            @size-change="handleSizeChange"
        />
    </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { getOperateLogs } from '@/api/log.js'

const logTable = reactive({
    query: { username: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: 500,
})

const getLogList = () => {
    getOperateLogs(logTable.query).then((res) => {
        if (res?.code === 200) {
            logTable.rows = res.data.rows
            logTable.total = res.data.total
        }
    })
}

const handleSearch = () => {
    logTable.query.page = 1
    getLogList()
}

const handleReset = () => {
    logTable.query.username = ''
    handleSearch()
}

const handleSizeChange = () => {
    logTable.query.page = 1
    getLogList()
}

onMounted(getLogList)
</script>
