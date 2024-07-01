<template>
    <el-form :model="form" label-width="auto" style="max-width: 600px">
        <el-form-item label="昵称">
            <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="用户名">
            <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
            <el-input v-model="form.password" />
        </el-form-item>
        <el-form-item label="手机号">
            <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="邮箱">
            <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item>
            <el-button>注销账号</el-button>
            <el-button @click="onSubmit">保存</el-button>
        </el-form-item>
    </el-form>
</template>
  
<script setup>
import { reactive } from 'vue'
import { updateUser } from '@/api/user.js'
import {ElMessage} from "element-plus";
const form = reactive(JSON.parse(localStorage.getItem("userinfo")))

const onSubmit = () => {
    updateUser(form).then(res => {
      if (res.code === 200) {
        ElMessage({message: '保存成功', type: 'success'})
      } else {
        ElMessage.error(`保存失败！${res.message}`)
      }
    })
}
</script>