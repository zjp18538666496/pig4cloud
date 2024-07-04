<script setup>
import {Lock, User} from "@element-plus/icons-vue";
import {reactive, ref} from 'vue'
import {ElMessage, ElNotification} from 'element-plus'
import {login} from "@/api/login.js";
import {useRouter} from "vue-router";
import {valiPassword, valiUsername} from "@/utils/validate.js";

const router = useRouter()
let loading = ref(false);
/**
 * 密码登录
 */
const login1 = () => {
  login({
    username: ruleForm.username,
    password: ruleForm.password
  }).then(res => {
    if (res?.code === 200) {
      ElMessage({message: '登录成功', type: 'success'})
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("userinfo", JSON.stringify(res.data.user));
      router.push('/')
    } else {
      ElMessage.error(`登录失败！${res.message}`)
    }
  }).finally(() => {
    loading.value = false
  })
}

/**
 * 验证码登录
 */
const codeLogin = () => {
  ElNotification.success({
    message: '开发中...',
    type: 'info',
    showClose: false,
  })
}

const emit = defineEmits(['dl'])

/**
 * 注册账号
 */
const registration = () => {
  emit('dl')
}

/**
 * 表单验证
 */
const ruleFormRef = ref()
const verifyUsername = (rule, value, callback) => {
  if (valiUsername(value)) {
    callback()
  } else {
    callback(new Error('请输入4到12位的用户名，支持字母合数字'))
  }

}
const verifyPassword = (rule, value, callback) => {
  if (valiPassword(value)) {
    callback()
  } else {
    callback(new Error('请输入4到18位的密码，支持字母、数字和特殊字符'))
  }
}

const ruleForm = reactive({
  username: '',
  password: ''
})

const rules = reactive({
  username: [{validator: verifyUsername, trigger: 'blur'}],
  password: [{validator: verifyPassword, trigger: 'blur'}]
})

/**
 * 提交表单
 */
const submitForm = (formEl) => {
  loading.value = true
  if (!formEl) {
    loading.value = false
    return
  }
  formEl.validate((valid) => {
    if (valid) {
      login1()
      return
    }
    loading.value = false
  })
}

</script>

<template>
  <el-form ref="ruleFormRef" style="max-width: 400px" :model="ruleForm" status-icon :rules="rules"
           label-width="auto" class="demo-ruleForm">
    <el-form-item prop="username">
      <el-input :prefix-icon="User" v-model="ruleForm.username" placeholder="请输入用户名" size="large" type="text"
                autocomplete="off" clearable/>
    </el-form-item>
    <el-form-item prop="password">
      <el-input :prefix-icon="Lock" v-model="ruleForm.password" placeholder="请输入用密码" size="large" type="password"
                autocomplete="off" show-password/>
    </el-form-item>
    <el-form-item class="pointer">
      <el-button :loading="loading" class="submitForm" size="large" type="primary" @click="submitForm(ruleFormRef)">
        登 录
      </el-button>
    </el-form-item>
    <div class="rest">
      <div @click="codeLogin">验证码登录</div>
      <div @click="registration">注册账号</div>
    </div>
  </el-form>
</template>

<style scoped>
.submitForm {
  width: 100%;
  background-color: #2E5CF6;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

form > div:last-child {
  margin-bottom: 0;
}

.pointer {
  cursor: pointer;
  padding-top: 15px;
}

.el-form-item {
  margin-bottom: 22px;
}

.rest {
  display: flex;
  justify-content: space-between;
  color: #2E5CF6;

  div {
    cursor: pointer;
  }
}
</style>