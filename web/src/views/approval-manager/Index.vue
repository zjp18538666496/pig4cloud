<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { applyApproval, completeApproval, getMyApprovals, getPendingApprovals } from '@/api/approval.js'
import { getRoleLists } from '@/api/role.js'

/**
 * 审批中心（Flowable）：我的申请 + 待我审批（角色申请场景）
 */
const activeTab = ref('my')

/**
 * 我的申请
 */
const myApps = ref([])
const applyVisible = ref(false)
const applyForm = reactive({ roleCode: '', reason: '' })
const roleList = ref([])

const loadMy = () => {
    getMyApprovals().then((res) => {
        if (res?.code === 200) {
            myApps.value = res.data
        }
    })
}

const openApply = () => {
    getRoleLists({}).then((res) => {
        if (res?.code === 200) {
            roleList.value = res.data.filter((role) => role.role_code !== 'super')
        }
    })
    applyVisible.value = true
}

const submitApply = () => {
    if (!applyForm.roleCode) {
        ElMessage.warning('请选择申请的角色')
        return
    }
    applyApproval(applyForm).then((res) => {
        if (res?.code === 200) {
            ElMessage.success(res.message || '申请已提交')
            applyVisible.value = false
            applyForm.roleCode = ''
            applyForm.reason = ''
            loadMy()
        } else {
            ElMessage.error(`提交失败！${res?.message}`)
        }
    })
}

const statusTag = (row) =>
    row.status === '审批中' ? 'warning' : row.status === '已通过' ? 'success' : 'danger'

/**
 * 待我审批
 */
const pending = ref([])
const loadPending = () => {
    getPendingApprovals().then((res) => {
        if (res?.code === 200) {
            pending.value = res.data
        }
    })
}

const complete = (row, approved) => {
    completeApproval({
        taskId: row.taskId,
        approved,
        comment: approved ? '' : '不符合条件，驳回',
    }).then((res) => {
        if (res?.code === 200) {
            ElMessage.success(res.message || '已处理')
            loadPending()
            loadMy()
        } else {
            ElMessage.error(`处理失败！${res?.message}`)
        }
    })
}

onMounted(() => {
    loadMy()
    loadPending()
})
</script>

<template>
    <div class="page">
        <el-tabs v-model="activeTab">
            <el-tab-pane label="我的申请" name="my">
                <div class="mb-10px">
                    <el-button type="primary" @click="openApply">申请角色</el-button>
                    <el-button @click="loadMy">刷新</el-button>
                </div>
                <el-table :data="myApps" border>
                    <el-table-column prop="applicantName" label="申请人" width="110" align="center" />
                    <el-table-column prop="roleName" label="申请角色" width="150" align="center" />
                    <el-table-column prop="reason" label="申请理由" min-width="180" show-overflow-tooltip />
                    <el-table-column label="状态" width="90" align="center">
                        <template #default="scope">
                            <el-tag :type="statusTag(scope.row)">{{ scope.row.status }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="审批人" width="110" align="center">
                        <template #default="scope">
                            <span v-if="scope.row.approver">{{ scope.row.approver }}</span>
                            <span v-else class="color-#909399">待审批</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="审批时间" width="170" align="center">
                        <template #default="scope">
                            {{ scope.row.approveTime || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="time" label="提交时间" width="170" align="center" />
                </el-table>
            </el-tab-pane>
            <el-tab-pane label="待我审批" name="pending">
                <div class="mb-10px">
                    <el-button @click="loadPending">刷新</el-button>
                </div>
                <el-table :data="pending" border>
                    <el-table-column prop="applicantName" label="申请人" width="110" align="center" />
                    <el-table-column prop="roleName" label="申请角色" width="150" align="center" />
                    <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
                    <el-table-column prop="time" label="提交时间" width="170" align="center" />
                    <el-table-column label="操作" width="150" align="center">
                        <template #default="scope">
                            <el-button size="small" type="success" @click="complete(scope.row, true)">通过</el-button>
                            <el-button size="small" type="danger" @click="complete(scope.row, false)">驳回</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>
        </el-tabs>

        <el-dialog v-model="applyVisible" title="申请角色" width="440" :close-on-click-modal="false">
            <el-form label-width="80px">
                <el-form-item label="角色">
                    <el-select v-model="applyForm.roleCode" placeholder="选择要申请的角色" style="width: 100%">
                        <el-option v-for="role in roleList" :key="role.id" :label="role.role_name" :value="role.role_code" />
                    </el-select>
                </el-form-item>
                <el-form-item label="申请理由">
                    <el-input v-model="applyForm.reason" type="textarea" :rows="3" placeholder="说明申请原因，便于审批人判断" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="applyVisible = false">取消</el-button>
                <el-button type="primary" @click="submitApply">提交申请</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
