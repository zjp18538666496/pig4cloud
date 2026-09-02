<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createNotice, delNotice, getNoticeLists, updateNotice } from '@/api/notice.js'

/**
 * 通知公告管理
 */
const noticeTable = reactive({
    query: { title: '', status: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getNoticeList = () => {
    getNoticeLists(noticeTable.query).then((res) => {
        if (res?.code === 200) {
            noticeTable.rows = res.data.rows
            noticeTable.total = res.data.total
        } else {
            ElMessage.error(`获取公告失败！${res?.message}`)
        }
    })
}
getNoticeList()

const handleSearch = () => {
    noticeTable.query.page = 1
    getNoticeList()
}

const handleReset = () => {
    noticeTable.query.title = ''
    noticeTable.query.status = ''
    handleSearch()
}

const handleSizeChange = () => {
    noticeTable.query.page = 1
    getNoticeList()
}

const statusText = (status) => ({ '0': '草稿', '1': '发布', '2': '定时' }[status] || status)

/**
 * 新增/编辑弹窗
 */
const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, title: '', content: '', status: '0' },
})
const formRef = ref()
const rules = reactive({
    title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
})

const openDialog = (type, row) => {
    dialog.type = type
    dialog.form = type === 'create'
        ? { id: null, title: '', content: '', status: '0', publish_time: null }
        : { id: row.id, title: row.title, content: row.content, status: row.status, publish_time: row.publish_time }
    dialog.visible = true
}

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) {
            return
        }
        const action = dialog.type === 'create' ? createNotice : updateNotice
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '保存成功')
                dialog.visible = false
                getNoticeList()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`您确定删除公告【${row.title}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        delNotice({ id: row.id }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '删除成功')
                getNoticeList()
            } else {
                ElMessage.error(`删除失败！${res?.message}`)
            }
        })
    })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between mb-10px">
            <div class="flex items-center gap-10px">
                <span>标题(模糊)：</span>
                <el-input class="w-200px" v-model="noticeTable.query.title" placeholder="请输入公告标题" clearable @keyup.enter="handleSearch" />
                <span>状态：</span>
                <el-select class="w-120px" v-model="noticeTable.query.status" placeholder="全部" clearable>
                    <el-option label="草稿" value="0" />
                    <el-option label="发布" value="1" />
                </el-select>
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" v-permission="['notice:write']" @click="openDialog('create')">发布公告</el-button>
        </div>
        <el-table :data="noticeTable.rows" border :max-height="noticeTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'info'">{{ statusText(scope.row.status) }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="create_by" label="发布人" width="120" />
            <el-table-column prop="create_time" label="创建时间" width="170" />
            <el-table-column label="操作" width="150" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link v-permission="['notice:write']" @click="openDialog('edit', scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link v-permission="['notice:remove']" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="noticeTable.query.page"
            v-model:page-size="noticeTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="noticeTable.total"
            @size-change="handleSizeChange"
            @current-change="getNoticeList"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '发布公告' : '编辑公告'" width="600" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="70px">
                <el-form-item label="标题" prop="title">
                    <el-input v-model="dialog.form.title" placeholder="请输入公告标题" maxlength="128" />
                </el-form-item>
                <el-form-item label="内容">
                    <el-input v-model="dialog.form.content" type="textarea" :rows="6" placeholder="请输入公告内容" maxlength="10000" show-word-limit />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="dialog.form.status">
                        <el-radio value="0">草稿（仅管理端可见）</el-radio>
                        <el-radio value="1">发布（用户端可见）</el-radio>
                        <el-radio value="2">定时发布</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item v-if="dialog.form.status === '2'" label="发布时间">
                    <el-date-picker v-model="dialog.form.publish_time" type="datetime" placeholder="到点自动发布" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
