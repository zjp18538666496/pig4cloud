<template>
  <el-form ref="ruleFormRef" :model="props.roleInfo" :rules="rules" label-width="auto" style="max-width: 600px">
    <el-form-item prop="roleName" label="角色名称">
      <el-input v-model="props.roleInfo.roleName"/>
    </el-form-item>
    <el-form-item prop="roleCode" label="角色编码">
      <el-input v-model="props.roleInfo.roleCode"/>
    </el-form-item>
    <el-form-item prop="description" label="角色描述">
      <el-input v-model="props.roleInfo.description"/>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {provide, ref} from "vue";

const props = defineProps({
  roleInfo: {
    type: Object,
    required: true
  },
  rules: {
    type: Object,
    required: true
  }
});
const ruleFormRef = ref(); // 创建一个 ref 引用

// 提供 formRef 实例
provide('ruleFormRef', ruleFormRef);
</script>