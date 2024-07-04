<script setup>
import {onMounted, onUnmounted, ref} from "vue";
import {delRole, getRoleLists} from "@/api/role.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {debounce} from "@/utils/utils.js";

let roleName = ref('')
let tableData = ref([])
let currentPage = ref(1)
let pageSize = ref(10)
let total = ref(0)
let background = ref(false)
let disabled = ref(false)
const computedTableHeight = ref(window.innerHeight - 50 - 30 - 40 - 52 - 52);
const getRoleList = () => {
  getRoleLists({
    page: currentPage.value,
    pageSize: pageSize.value,
    roleName: roleName.value
  }).then(res => {
    tableData.value = res.data.rows;
    total.value = res.data.total;
    pageSize.value = res.data.size;
  })
}
getRoleList()
const handleEdit = (index, row) => {
  //getRoleList()
}
const handleDelete = (index, row) => {
  ElMessageBox.confirm(`您确定删除【${row.roleName}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
      .then(() => {
        delRole({
          roleCode: row.roleCode
        }).then(res => {
          if (res?.code === 200) {
            ElMessage({message: '删除成功', type: 'success'})
            getRoleList()
          } else {
            ElMessage.error(`删除失败！${res.message}`)
          }
        })
      })
      .catch(() => {

      });
}
const updateTableHeight = () => {
  computedTableHeight.value = window.innerHeight - 50 - 30 - 40 - 52 - 52; // 根据实际情况调整
};
onMounted(() => {
  // 监听窗口大小改变，重新计算表格高度
  const updateTableHeightFunc = debounce(updateTableHeight, 500);
  window.addEventListener('resize', updateTableHeightFunc);
});

onUnmounted(() => {
  window.removeEventListener('resize', updateTableHeight);
});


</script>

<template>
  <div>
    <div style="margin-bottom: 20px;">
      角色名称
      <el-input v-model="roleName" style="width: 240px" placeholder="角色名称"/>
      <el-button style="margin: 0 0 0 10px;" @click="getRoleList">查询</el-button>
      <el-button type="primary" @click="roleName=''">重置</el-button>
    </div>
    <el-table :data="tableData" border style="width: 100%; overflow: auto;" :max-height="computedTableHeight">
      <el-table-column prop="date" label="序号">
        <template #default="scope">
          <span>{{ scope.$index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="roleName" label="角色名称"/>
      <el-table-column prop="roleCode" label="角色标识"/>
      <el-table-column prop="description" label="角色描述"/>
      <el-table-column prop="address" label="数据权限"/>
      <el-table-column prop="address" label="创建时间"/>
      <el-table-column prop="address" label="操作">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.$index, scope.row)">
            编辑
          </el-button>
          <el-button
              size="small"
              type="danger"
              @click="handleDelete(scope.$index, scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display: flex; justify-content: flex-end; width: 100%;">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 40]"
          style="margin-top: 20px;"
          :disabled="disabled"
          :background="background"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="getRoleList"
          @current-change="getRoleList"
      />
    </div>
  </div>
</template>

<style scoped>

</style>