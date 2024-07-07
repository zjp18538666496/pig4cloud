<template>
  <el-form ref="ruleFormRef" :model="props.roleInfo" :rules="rules" label-width="auto" style="max-width: 600px">
    <el-form-item label="昵称">
      <el-input v-model="props.roleInfo.name"/>
    </el-form-item>
    <el-form-item label="用户名">
      <el-input disabled v-model="props.roleInfo.username"/>
    </el-form-item>
    <el-form-item label="手机号">
      <el-input v-model="props.roleInfo.mobile"/>
    </el-form-item>
    <el-form-item label="邮箱">
      <el-input v-model="props.roleInfo.email"/>
    </el-form-item>
    <el-select
        v-model="props.roleInfo.role_codes"
        multiple
        placeholder="Select"
        @change="(values) => props.roleInfo.role_codes = values"
        @clear="() => props.roleInfo.role_codes = []"
    >
      <el-option
          v-for="item in roleLists"
          :key="item.role_code"
          :label="item.role_name"
          :value="item.role_code"
      />
    </el-select>
    <el-form-item label="更新时间">
      <el-date-picker
          v-model="props.roleInfo.updateTime"
          type="datetime"
          placeholder="更新时间"
          disabled
      />
    </el-form-item>
    <el-form-item label="创建时间">
      <el-date-picker
          v-model="props.roleInfo.createTime"
          type="datetime"
          placeholder="创建时间"
          disabled
      />
    </el-form-item>
    <el-form-item label="最后登录时间">
      <el-date-picker
          v-model="props.roleInfo.lastLoginTime"
          type="datetime"
          placeholder="最后登录时间"
          disabled
      />
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ref} from "vue";
import {getRoleLists} from "@/api/role.js";

const roleLists = ref([]);
const ruleFormRef = ref();
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
getRoleLists({}).then(res => {
  if (res.code === 200) {
    roleLists.value = res.data;
  }
})
defineExpose({
  ruleFormRef
})
</script>