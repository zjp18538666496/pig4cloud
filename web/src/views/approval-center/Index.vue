<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approve, getApprovals } from '@/api/approval.js'

/**
 * 审批中心（super）：待审单列表 + 通过/驳回（驳回需填意见）
 */
const typeMap = { role_apply: '角色申请', tenant_open: '租户开通', handover: '离职交接' }
const statusTag = (s) => (s === '0' ? 'warning' : s === '1' ? 'success' : 'danger')
const statusText = (s) => (s === '0' ? '待审批' : s === '1' ? '已通过' : '已驳回')

const table = reactive({ query: { status: '', applicant: '', page: 1, pageSize: 10 }, rows: [], total: 0, loading: false })

const loadList = () => {
    table.loading = true
    getApprovals(table.query).then((res) => {
        if (res?.code === 200) { table.rows = res.data.rows; table.total = res.data.total }
    }).finally(() => { table.loading = false })
}
onMounted(loadList)

const doApprove = (row, pass) => {
    ElMessageBox.prompt(
        pass ? `通过【${row.applicant}】的申请：${row.title}？可填审批意见。` : `驳回【${row.applicant}】的申请：${row.title}？请填写驳回原因。`,
        pass ? '通过申请' : '驳回申请',
        { inputPlaceholder: '审批意见（' + (pass ? '选填' : '必填') + '）', confirmButtonText: pass ? '确认通过' : '确认驳回' },
    ).then(({ value }) => {
        if (!pass && !value) { ElMessage.error('驳回必须填写原因'); return }
        approve({ id: row.id, pass, comment: value || '' }).then((res) => {
            if (res?.code === 200) { ElMessage.success('审批完成'); loadList() } else ElMessage.error(res?.message)
        })
    }).catch(() => { })
}
</script>

<template>
    <div class="page">
        <div class="flex items-center gap-10px mb-10px">
            <el-select v-model="table.query.status" placeholder="全部状态" clearable style="width: 130px" @change="table.query.page = 1; loadList()">
                <el-option label="待审批" value="0" />
                <el-option label="已通过" value="1" />
                <el-option label="已驳回" value="2" />
            </el-select>
            <el-input class="w-180px" v-model="table.query.applicant" placeholder="申请人" clearable @keyup.enter="table.query.page = 1; loadList()" />
            <el-button type="primary" @click="table.query.page = 1; loadList()">查询</el-button>
        </div>
        <el-table :data="table.rows" border v-loading="table.loading">
            <el-table-column prop="id" label="单号" width="70" align="center" />
            <el-table-column prop="title" label="申请标题" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="100" align="center">
                <template #default="scope">{{ typeMap[scope.row.apply_type] || scope.row.apply_type }}</template>
            </el-table-column>
            <el-table-column prop="applicant" label="申请人" width="110" />
            <el-table-column prop="reason" label="申请理由" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="statusTag(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="approver" label="审批人" width="100" />
            <el-table-column prop="approve_comment" label="审批意见" min-width="130" show-overflow-tooltip />
            <el-table-column prop="create_time" label="申请时间" width="165" align="center" />
            <el-table-column label="操作" width="150" align="center">
                <template #default="scope">
                    <template v-if="scope.row.status === '0'">
                        <el-button size="small" type="success" @click="doApprove(scope.row, true)">通过</el-button>
                        <el-button size="small" type="danger" @click="doApprove(scope.row, false)">驳回</el-button>
                    </template>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination class="mt-10px flex justify-end" v-model:current-page="table.query.page"
            v-model:page-size="table.query.pageSize" :total="table.total" layout="total, prev, pager, next"
            @current-change="loadList" />
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
