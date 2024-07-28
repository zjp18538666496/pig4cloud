<template>
    <el-tabs v-model="activeName" class="demo-tabs">
        <el-tab-pane label="基本信息" name="basic">
            <el-form :model="form" label-width="auto" style="max-width: 600px">
                <el-form-item label="  ">
                    <el-avatar :size="150" shape="circle" :src="baseUrl + form.avatar" />
                </el-form-item>
                <el-form-item label="昵称">
                    <el-input v-model="form.name" />
                </el-form-item>
                <el-form-item label="用户名">
                    <el-input disabled v-model="form.username" />
                </el-form-item>
                <el-form-item label="手机号">
                    <el-input v-model="form.mobile" />
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="form.email" />
                </el-form-item>
                <el-form-item label="更新时间">
                    <el-date-picker v-model="form.update_time" type="datetime" placeholder="更新时间" disabled />
                </el-form-item>
                <el-form-item label="创建时间">
                    <el-date-picker v-model="form.create_time" type="datetime" placeholder="创建时间" disabled />
                </el-form-item>
                <el-form-item label="最后登录时间">
                    <el-date-picker v-model="form.last_login_time" type="datetime" placeholder="最后登录时间" disabled />
                </el-form-item>
                <el-form-item>
                    <el-button @click="logOut">注销账号</el-button>
                    <el-button @click="onSubmit">更新个人信息</el-button>
                </el-form-item>
            </el-form>
        </el-tab-pane>
        <el-tab-pane label="安全信息" name="second">
            <el-form ref="formEl" :rules="rules" :model="passwordForm" label-width="auto" style="max-width: 600px">
                <el-form-item prop="password" label="原密码">
                    <el-input type="password" v-model="passwordForm.password" show-password />
                </el-form-item>
                <el-form-item prop="newPassword" label="新密码">
                    <el-input type="password" v-model="passwordForm.newPassword" show-password />
                </el-form-item>
                <el-form-item prop="confirmPassword" label="确认密码">
                    <el-input type="password" v-model="passwordForm.confirmPassword" show-password />
                </el-form-item>
                <el-form-item>
                    <el-button :loading="loading" @click="submitForm(formEl)">更新密码</el-button>
                </el-form-item>
            </el-form>
        </el-tab-pane>
    </el-tabs>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { delUser, updatePassword, updateUser } from '@/api/user.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VerifyUser } from '@/utils/vali.js'
const verifyUser = new VerifyUser()
const baseUrl = import.meta.env.VITE_BASE_URL
const router = useRouter()
const activeName = ref('basic')
const formEl = ref()
let loading = ref(false)
let form = ref(JSON.parse(localStorage.getItem('userinfo')))
const passwordForm = reactive({
    password: '',
    newPassword: '',
    confirmPassword: '',
})
const onSubmit = () => {
    updateUser(form.value).then((res) => {
        if (!res) return
        if (res.code === 200) {
            form.value = res.data
            localStorage.setItem('userinfo', JSON.stringify(res.data))
            ElMessage({ message: '保存成功', type: 'success' })
        } else {
            ElMessage.error(`保存失败！${res.message}`)
        }
    })
}
const logOut = () => {
    ElMessageBox.confirm('注销账号后无法恢复，确定注销吗？', '提示', {
        confirmButtonText: '注销',
        cancelButtonText: '取消',
        type: 'info',
    })
        .then(() => delUser({ username: form.username }))
        .then((res) => {
            if (!res) return
            if (res.code === 200) {
                localStorage.clear()
                ElMessage({ message: '注销成功', type: 'success' })
                router.push('/login')
            }
        })
        .catch(() => {})
}

const rules = reactive({
    password: [{ validator: verifyUser.password, trigger: 'blur' }],
    newPassword: [
        {
            validator: (rule, value, callback) => {
                verifyUser.newPassword(rule, value, callback, passwordForm)
            },
            trigger: 'blur',
        },
    ],
    confirmPassword: [
        {
            validator: (rule, value, callback) => {
                verifyUser.confirmPassword(rule, value, callback, passwordForm)
            },
            trigger: 'blur',
        },
    ],
})

const updatePassword1 = () => {
    updatePassword({
        password: passwordForm.password,
        newPassword: passwordForm.newPassword,
    })
        .then((res) => {
            if (res?.code === 200) {
                ElMessage({ message: '更新成功', type: 'success' })
            } else {
                ElMessage.error(`更新失败！${res.message}`)
            }
        })
        .finally(() => {
            loading.value = false
        })
}

const submitForm = (formEl) => {
    loading.value = true
    if (!formEl) {
        loading.value = false
        return
    }
    formEl.validate((valid) => {
        if (valid) {
            updatePassword1()
            return
        }
        loading.value = false
    })
}
</script>
