<template>
  <button @click="test1">测试</button>
</template>
<script setup>
import {getUser} from "@/api/user.js";


const test1 = () => {
  getUser(1).then(res => {
    console.log(res);
  });
}
</script>
<style scoped>
</style>
