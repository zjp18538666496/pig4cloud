<script setup>
import {onMounted, onUnmounted, reactive, ref} from "vue";
import {ElMessage, ElMessageBox} from "element-plus";
import EditMenu from "@/views/menu-manager/components/EditMenu.vue";
import {debounce} from "@/utils/utils.js";
import {createUser, delUser, updateUser} from "@/api/user.js";
import {getMenuLists} from "@/api/menu.js";

let userTable = ref({
  // 表格数据
  rows: [],
  // 表格高度
  height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
  // 查询条件
  query: {
    menu_name: ''
  },
  // 表格分页组件属性
  pagination: {
    total: 0,
    disabled: false,
    background: false,
  }
});


let roleRef = ref();

let user = ref({
  roleInfo: null,
  dialogVisible: false,
})

const initRoleInfo = () => {
  user.value.roleInfo = {}
}

let type = null;
/**
 * 表单验证
 */
const verifyUsername = (rule, value, callback) => {
  if (value) {
    callback()
  } else {
    callback(new Error('用户名称不能为空'))
  }

}
const verifyPassword = (rule, value, callback) => {
  if (value) {
    callback()
  } else {
    callback(new Error('用户编码不能为空'))
  }
}
const rules = reactive({
  roleName: [{validator: verifyUsername, trigger: 'blur'}],
  roleCode: [{validator: verifyPassword, trigger: 'blur'}],
})
/**
 * 获取用户列表
 */
const getUserLise = () => {
  getMenuLists(userTable.value.query).then(res => {
    if (res?.code === 200) {
      userTable.value.rows = res.data;
      // userTable.value.pagination.total = res.data.total
      // userTable.value.query.pageSize = res.data.size
    }
  })
}

getUserLise()


/**
 * 更新表格高度
 */
const updateTableHeight = () => {
  userTable.value.height = window.innerHeight - 50 - 30 - 40 - 52 - 52; // 根据实际情况调整
};


/*
 * 弹窗关闭
 */
const handleClose = (() => {
  roleRef.value.ruleFormRef.resetFields()
  setTimeout(() => user.value.dialogVisible = false)
  initRoleInfo()
})

const createRole1 = () => {
  type = 'create'
  initRoleInfo()
  user.value.dialogVisible = true
}

/**
 * 删除
 * @param index
 * @param row
 */
const handleDelete = (index, row) => {
  ElMessageBox.confirm(`您确定删除【${row.name}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    delUser(row).then(res => {
      if (res?.code === 200) {
        ElMessage({message: '删除成功', type: 'success'})
        getUserLise()
      } else {
        ElMessage.error(`删除失败！${res.message}`)
      }
    })
  }).catch(() => {

  });
}

/**
 * 编辑
 * @param index
 * @param row
 */
const handleEdit = (index, row) => {
  type = 'edit'
  user.value.roleInfo = {...row}
  user.value.roleInfo.role_codes = row.role_codes?.split(',').filter(item => item !== '')
  user.value.dialogVisible = true
}

/**
 * 保存用户
 */
const saveRole = () => {
  roleRef.value.ruleFormRef.validate((valid) => {
    if (valid) {
      user.value.roleInfo.role_codes = user.value.roleInfo.role_codes?.join(',')
      switch (type) {
        case 'create':
          createUser(user.value.roleInfo).then(res => {
            if (res?.code === 200) {
              user.value.dialogVisible = false
              initRoleInfo()
              ElMessage({message: '保存成功', type: 'success'})
              getUserLise()
            } else {
              ElMessage.error(`保存失败！${res.message}`)
            }
          })
          break;
        case 'edit':
          updateUser(user.value.roleInfo).then(res => {
            if (res?.code === 200) {
              user.value.dialogVisible = false
              initRoleInfo()
              ElMessage({message: '保存成功', type: 'success'})
              getUserLise()
            } else {
              ElMessage.error(`保存失败！${res.message}`)
            }
          })
          break;
        default:
          break;
      }
    }
  })
}

/**
 * 监听窗口大小改变，重新计算表格高度
 */
onMounted(() => {
  // 监听窗口大小改变，重新计算表格高度
  const updateTableHeightFunc = debounce(updateTableHeight, 500);
  window.addEventListener('resize', updateTableHeightFunc);
});

/**
 * 监听窗口大小改变，重新计算表格高度
 */
onUnmounted(() => {
  window.removeEventListener('resize', updateTableHeight);
});
</script>

<template>
  <div>
    <div style="margin-bottom: 20px;">
      菜单名称
      <el-input v-model="userTable.query.menu_name" style="width: 240px" placeholder="菜单名称"/>
      <el-button style="margin: 0 0 0 10px;" @click="getUserLise">查询</el-button>
      <el-button type="primary" @click="userTable.query.menu_name=''">重置</el-button>
      <el-button type="primary" @click="createRole1">新增</el-button>
    </div>
    <el-table :data="userTable.rows"
              border style="width: 100%;
              overflow: auto;"
              :max-height="userTable.height"
              row-key="id"
    >
      <el-table-column prop="menu_name" label="菜单名称" align="center"/>
      <el-table-column prop="route" label="路由地址" align="left">
        <template #default="scope">
          <el-tag type="primary">{{ scope.row.route }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'danger' : 'primary'">
            {{
              {
                "0": "禁用",
                "1": "启用"
              }[scope.row.status] || ""
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="菜单类型" align="center">
        <template #default="scope">
          <el-tag type="primary">
            {{
              {
                "0": "目录",
                "1": "菜单",
                "2": "按钮"
              }[scope.row.type] || ""
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="level" label="层级" align="center"/>
      <el-table-column prop="address" label="操作" align="center">
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
    <div>
      <el-dialog
          v-model="user.dialogVisible"
          title="编辑菜单"
          width="800"
          :before-close="handleClose"
      >
        <EditMenu
            :rules="rules"
            :roleInfo="user.roleInfo"
            ref="roleRef"
        />
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="user.dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveRole">
              保存
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>

</style>