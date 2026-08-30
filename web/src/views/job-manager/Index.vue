<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createJob, delJob, getJobLists, getJobLogs, runJobOnce, updateJob } from '@/api/job.js'

/**
 * 定时任务管理：CRUD/启停/手动执行/执行日志
 */
const jobTable = reactive({
    query: { job_name: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getJobList = () => {
    getJobLists(jobTable.query).then((res) => {
        if (res?.code === 200) {
            jobTable.rows = res.data.rows
            jobTable.total = res.data.total
        } else {
            ElMessage.error(`获取任务失败！${res?.message}`)
        }
    })
}
getJobList()

const handleSearch = () => {
    jobTable.query.page = 1
    getJobList()
}
const handleReset = () => {
    jobTable.query.job_name = ''
    handleSearch()
}
const handleSizeChange = () => {
    jobTable.query.page = 1
    getJobList()
}

const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, job_name: '', handler: '', cron: '', status: '0', remark: '' },
})
const formRef = ref()
const rules = reactive({
    job_name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
    handler: [{ required: true, message: '请输入处理器名', trigger: 'blur' }],
    cron: [{ required: true, message: '请输入cron表达式', trigger: 'blur' }],
})

const openDialog = (type, row) => {
    dialog.type = type
    dialog.form = type === 'create'
        ? { id: null, job_name: '', handler: '', cron: '', status: '0', remark: '' }
        : { id: row.id, job_name: row.job_name, handler: row.handler, cron: row.cron, status: row.status, remark: row.remark }
    dialog.visible = true
}

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        const action = dialog.type === 'create' ? createJob : updateJob
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '保存成功')
                dialog.visible = false
                getJobList()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`删除任务【${row.job_name}】会同时删除其执行日志，您确定吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        delJob({ id: row.id }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '删除成功')
                getJobList()
            } else {
                ElMessage.error(`删除失败！${res?.message}`)
            }
        })
    })
}

/**
 * 手动执行一次
 */
const handleRun = (row) => {
    runJobOnce({ id: row.id }).then((res) => {
        if (res?.code === 200) {
            ElMessage.success(`任务执行结果：${res.data}`)
            getJobList()
        } else {
            ElMessage.error(`执行失败！${res?.message}`)
        }
    })
}

/**
 * 执行日志抽屉
 */
const logDrawer = reactive({
    visible: false,
    jobName: '',
    logs: [],
})
const openLogs = (row) => {
    getJobLogs(row.id).then((res) => {
        if (res?.code === 200) {
            logDrawer.logs = res.data || []
            logDrawer.jobName = row.job_name
            logDrawer.visible = true
        } else {
            ElMessage.error(`获取日志失败！${res?.message}`)
        }
    })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between mb-10px">
            <div class="flex items-center gap-10px">
                <span>任务名称：</span>
                <el-input class="w-200px" v-model="jobTable.query.job_name" placeholder="请输入任务名称" clearable @keyup.enter="handleSearch" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" v-permission="['job:write']" @click="openDialog('create')">新增任务</el-button>
        </div>
        <el-table :data="jobTable.rows" border :max-height="jobTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="job_name" label="任务名称" min-width="150" />
            <el-table-column prop="handler" label="处理器" min-width="160" />
            <el-table-column prop="cron" label="cron表达式" width="140" />
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'info'">{{ scope.row.status === '1' ? '启用' : '停用' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="260" align="center">
                <template #default="scope">
                    <el-button size="small" type="success" link v-permission="['job:run']" @click="handleRun(scope.row)">执行</el-button>
                    <el-button size="small" type="primary" link v-permission="['job:write']" @click="openLogs(scope.row)">日志</el-button>
                    <el-button size="small" type="primary" link v-permission="['job:write']" @click="openDialog('edit', scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link v-permission="['job:remove']" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="jobTable.query.page"
            v-model:page-size="jobTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="jobTable.total"
            @size-change="handleSizeChange"
            @current-change="getJobList"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '新增任务' : '编辑任务'" width="520" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="100px">
                <el-form-item label="任务名称" prop="job_name">
                    <el-input v-model="dialog.form.job_name" maxlength="64" />
                </el-form-item>
                <el-form-item label="处理器" prop="handler">
                    <el-input v-model="dialog.form.handler" placeholder="如tenantExpireCheckJob" maxlength="64" />
                </el-form-item>
                <el-form-item label="cron表达式" prop="cron">
                    <el-input v-model="dialog.form.cron" placeholder="如 0 0 1 * * ?" maxlength="32" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="dialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="dialog.form.remark" type="textarea" :rows="2" maxlength="255" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>

        <el-drawer v-model="logDrawer.visible" :title="`执行日志 - ${logDrawer.jobName}`" size="620">
            <el-empty v-if="!logDrawer.logs.length" description="暂无执行记录" :image-size="60" />
            <el-table v-else :data="logDrawer.logs" border size="small">
                <el-table-column prop="create_time" label="执行时间" width="160" />
                <el-table-column label="结果" width="70" align="center">
                    <template #default="scope">
                        <el-tag :type="scope.row.success === '1' ? 'success' : 'danger'" size="small">
                            {{ scope.row.success === '1' ? '成功' : '失败' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="cost_ms" label="耗时(ms)" width="90" align="center" />
                <el-table-column prop="message" label="信息" show-overflow-tooltip />
            </el-table>
        </el-drawer>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
