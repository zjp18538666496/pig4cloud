<template>
  <div class="sidebar">
    <div class="logo" :style="borderRight" @click="toggleCollapse">
      {{ logo }}
    </div>
    <el-menu
        :default-active="defaultActive || '/home'"
        class="el-menu-vertical-demo"
        @open="handleOpen"
        @close="handleClose"
        @select="handleSelect"
        :collapse="isCollapse"
        :router="true"
    >
      <el-menu-item index="/home">
        <el-icon>
          <icon-menu/>
        </el-icon>
        <span>首页</span>
      </el-menu-item>
      <el-sub-menu index="1">
        <template #title>
          <el-icon>
            <icon-menu/>
          </el-icon>
          <span>系统管理</span>
        </template>
        <el-sub-menu index="2">
          <template #title>
            <el-icon>
              <icon-menu/>
            </el-icon>
            <span>权限管理</span>
          </template>
          <el-menu-item index="/user-manager">
            <el-icon>
              <icon-menu/>
            </el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="role-manager">
            <el-icon>
              <icon-menu/>
            </el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/menu-manager">
            <el-icon>
              <icon-menu/>
            </el-icon>
            <span>菜单管理</span>
          </el-menu-item>
          <el-menu-item index="1-2">
            <el-icon>
              <icon-menu/>
            </el-icon>
            <span>部门管理</span>
          </el-menu-item>
          <el-menu-item index="1-2">
            <el-icon>
              <icon-menu/>
            </el-icon>
            <span>岗位管理</span>
          </el-menu-item>
          <el-menu-item index="1-2">
            <el-icon>
              <icon-menu/>
            </el-icon>
            <span>租户管理</span>
          </el-menu-item>
        </el-sub-menu>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import {Menu as IconMenu,} from '@element-plus/icons-vue'
import {storeToRefs} from 'pinia'
import {useSidebarStore} from '@/stores/sidebar.js'
const store = useSidebarStore()
let {isCollapse, width, logo, borderRight, defaultActive} = storeToRefs(store)

/**
 * 折叠面板
 */
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
  width.value = isCollapse.value ? '63px' : '200px'
  borderRight.value = isCollapse.value ? {
    borderRight: 'none'
  } : {
    borderRight: '1px solid #9a9a9a'
  }
  if (isCollapse.value) {
    logo.value = 'PIGX'
  } else {
    setTimeout(() => {
      logo.value = 'PIGX ADMIN'
    }, 500)
  }
}

const handleOpen = (key, keyPath) => {
  console.log(key, keyPath)
}
const handleClose = (key, keyPath) => {
  console.log(key, keyPath)
}
const handleSelect = (key, keyPath) => {
  console.log(key, keyPath)
  defaultActive.value = `/${key}`
}
</script>
<style scoped lang="scss">
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;

  .logo {
    width: 100%;
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: #00152905 0 1px 4px;
    background-color: #2e5cf6;
    font-size: 16px;
    color: #fff;
    cursor: pointer;
    border-right: 1px solid #9a9a9a;
  }

  .el-menu {
    flex: 1;
  }
}
</style>
