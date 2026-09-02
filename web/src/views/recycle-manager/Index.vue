<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRecycleLists, purgeRecycle, restoreRecycle } from '@/api/recycle.js'

/**
 * 回收站：软删除的用户/角色可恢复或彻底删除
 */
const table = reactive({ rows: [], height: window.innerHeight - 50 - 30 - 40 - 52 - 52 })

const loadList = () => {
    getRecycleLists().then((res) => {
        if (res?.code === 200) {
            table.rows = res.data
        } else {
            ElMessage.error(`获取回收站失败！${res?.message}`)
        }
    })
}
loadList()

const handleRestore = (row) => {
    ElMessageBox.confirm(`确定恢复【${row.item_type === 'user' ? row.username : row.role_name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
    }).then(() => {
        restoreRecycle({ id: row.id, type: row.item_type }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '恢复成功')
                loadList()
            } else {
                ElMessage.error(`恢复失败！${res?.message}`)
            }
        })
    })
}

const handlePurge = (row) => {
    const name = row.item_type === 'user' ? row.username : row.role_name
    ElMessageBox.confirm(`彻底删除【${name}】后不可恢复，相关关联数据将一并清除，确定吗？`, '危险操作', {
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        purgeRecycle({ id: row.id, type: row.item_type }).then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '已彻底删除')
                loadList()
            } else {
                ElMessage.error(`删除失败！${res?.message}`)
            }
        })
    })
}
</script>

<template>
    <div class="page">
        <div class="flex justify-between items-center mb-10px">
            <span>回收站（删除的用户/角色，可恢复或彻底删除）</span>
            <el-button type="primary" @click="loadList">刷新</el-button>
        </div>
        <el-table :data="table.rows" border :max-height="table.height">
            <el-table-column label="类型" width="90" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.item_type === 'user' ? 'primary' : 'warning'">
                        {{ scope.row.item_type === 'user' ? '用户' : '角色' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="名称" min-width="140">
                <template #default="scope">
                    {{ scope.row.item_type === 'user' ? scope.row.username : scope.row.role_name }}
                </template>
            </el-table-column>
            <el-table-column label="标识" min-width="120">
                <template #default="scope">
                    {{ scope.row.item_type === 'user' ? (scope.row.name || '-') : scope.row.role_code }}
                </template>
            </el-table-column>
            <el-table-column prop="tenant_name" label="所属租户" width="140" align="center">
                <template #default="scope">{{ scope.row.tenant_name || `平台(${scope.row.tenant_id})` }}</template>
            </el-table-column>
            <el-table-column prop="delete_time" label="删除时间" width="170" align="center" />
            <el-table-column label="操作" width="170" align="center">
                <template #default="scope">
                    <el-button size="small" type="success" link v-permission="['recycle:manage']" @click="handleRestore(scope.row)">恢复</el-button>
                    <el-button size="small" type="danger" link v-permission="['recycle:manage']" @click="handlePurge(scope.row)">彻底删除</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}
</style>
