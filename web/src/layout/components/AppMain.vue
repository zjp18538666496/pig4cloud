<script setup>

</script>

<template>
  <div class="main-container">
    <router-view/>
  </div>
</template>

<style scoped>
.main-container {
  padding: 20px;
  width: 100%;
  height: calc(100vh - 80px);
  overflow: auto;
  border-radius: 8px;
  background-color: #fff;
}
</style>
