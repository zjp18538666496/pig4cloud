<script setup>
import { useRouter } from "vue-router";
const router = useRouter()
  const backHome = () => {
    router.push("/")
  }
</script>

<template>
  <div class="page-404">
    <el-empty :image-size="200" description="404">
      <el-button type="primary" @click="backHome">返回首页</el-button>
    </el-empty>
  </div>
</template>
<style scoped>
.page-404 {
  display: flex;
  justify-content: center;
  align-items: center;
  padding-bottom: 100px;
  width: 100vw;
  height: 100vh;
}
</style>
