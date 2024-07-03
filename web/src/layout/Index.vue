<template>
  <div class="common-layout">
    <el-container>
      <el-aside :width="width">
        <Sidebar/>
      </el-aside>
      <el-container>
        <el-header>
          <Header/>
        </el-header>
        <el-main class="app_main">
          <AppMain/>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>


<script setup>
import Sidebar from '@/layout/components/sidebar/Index.vue'
import AppMain from '@/layout/components/AppMain.vue'
import Header from '@/layout/components/header/Index.vue'
import {storeToRefs} from 'pinia'
import {useSidebarStore} from '@/stores/sidebar.js'

const store = useSidebarStore()
let {width} = storeToRefs(store)
</script>
<style scoped>
.el-header {
  height: 50px;
  background-color: #2e5cf6;
}

.el-aside {
  overflow: hidden;
  transition: width 0.3s ease-in-out;
}

.app_main {
  padding: 15px;
  width: 100%;
  height: calc(100vh - 50px);
  background-color: rgb(248, 248, 248);
  overflow: hidden;
}

.common-layout {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
</style>
