<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPost, delPost, getPostLists, updatePost } from '@/api/post.js'

/**
 * 岗位管理（租户隔离）
 */
const postTable = reactive({
    query: { post_name: '', page: 1, pageSize: 10 },
    rows: [],
    total: 0,
    height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
})

const getPostList = () => {
    getPostLists(postTable.query).then((res) => {
        if (res?.code === 200) {
            postTable.rows = res.data.rows
            postTable.total = res.data.total
        } else {
            ElMessage.error(`获取岗位失败！${res?.message}`)
        }
    })
}
getPostList()

const handleSearch = () => {
    postTable.query.page = 1
    getPostList()
}
const handleReset = () => {
    postTable.query.post_name = ''
    handleSearch()
}
const handleSizeChange = () => {
    postTable.query.page = 1
    getPostList()
}

const dialog = reactive({
    visible: false,
    type: 'create',
    form: { id: null, post_code: '', post_name: '', sort: 0, status: '1' },
})
const formRef = ref()
const rules = reactive({
    post_code: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
    post_name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
})

const openDialog = (type, row) => {
    dialog.type = type
    dialog.form = type === 'create'
        ? { id: null, post_code: '', post_name: '', sort: 0, status: '1' }
        : { id: row.id, post_code: row.post_code, post_name: row.post_name, sort: row.sort, status: row.status }
    dialog.visible = true
}

const save = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        const action = dialog.type === 'create' ? createPost : updatePost
        action(dialog.form).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '保存成功')
                dialog.visible = false
                getPostList()
            } else {
                ElMessage.error(`保存失败！${res?.message}`)
            }
        })
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`您确定删除岗位【${row.post_name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        delPost({ id: row.id }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '删除成功')
                getPostList()
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
                <span>岗位名称：</span>
                <el-input class="w-200px" v-model="postTable.query.post_name" placeholder="请输入岗位名称" clearable @keyup.enter="handleSearch" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" v-permission="['post:write']" @click="openDialog('create')">新增岗位</el-button>
        </div>
        <el-table :data="postTable.rows" border :max-height="postTable.height">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column prop="post_code" label="岗位编码" min-width="140" />
            <el-table-column prop="post_name" label="岗位名称" min-width="140" />
            <el-table-column prop="sort" label="排序" width="90" align="center" />
            <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">{{ scope.row.status === '1' ? '启用' : '停用' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="create_time" label="创建时间" width="170" />
            <el-table-column label="操作" width="160" align="center">
                <template #default="scope">
                    <el-button size="small" type="primary" link v-permission="['post:write']" @click="openDialog('edit', scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link v-permission="['post:remove']" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            class="mt-10px flex justify-end"
            v-model:current-page="postTable.query.page"
            v-model:page-size="postTable.query.pageSize"
            :page-sizes="[10, 20, 30, 40, 50]"
            :background="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="postTable.total"
            @size-change="handleSizeChange"
            @current-change="getPostList"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.type === 'create' ? '新增岗位' : '编辑岗位'" width="480" :close-on-click-modal="false">
            <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
                <el-form-item label="岗位编码" prop="post_code">
                    <el-input v-model="dialog.form.post_code" :disabled="dialog.type === 'edit'" maxlength="64" />
                </el-form-item>
                <el-form-item label="岗位名称" prop="post_name">
                    <el-input v-model="dialog.form.post_name" maxlength="64" />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="dialog.form.sort" :min="0" :max="999" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="dialog.form.status">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">停用</el-radio>
                    </el-radio-group>
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
