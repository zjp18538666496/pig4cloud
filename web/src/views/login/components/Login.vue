<script setup>
import { Lock, User } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/login.js'
import { getCaptcha, resetPasswordByEmail, sendResetCode } from '@/api/auth.js'
import { getPolicy } from '@/api/config.js'
import { useRouter } from 'vue-router'
import { VerifyUser } from '@/utils/vali.js'

const router = useRouter()
let loading = ref(false)
const verifyUser = new VerifyUser()

/**
 * 密码策略/验证码开关（sys_config可配）
 */
const policy = reactive({ captchaEnabled: true, minLength: 8 })
getPolicy().then((res) => {
    if (res?.code === 200) {
        Object.assign(policy, res.data)
    }
})

/**
 * 图形验证码
 */
const captcha = reactive({
    id: '',
    image: '',
})
const loadCaptcha = () => {
    getCaptcha().then((res) => {
        if (res?.code === 200) {
            captcha.id = res.data.captchaId
            captcha.image = res.data.image
            ruleForm.captchaCode = ''
        }
    })
}
loadCaptcha()

/**
 * 密码登录
 */
const login1 = () => {
    login({
        username: ruleForm.username,
        password: ruleForm.password,
        captchaId: captcha.id,
        captchaCode: ruleForm.captchaCode,
        totpCode: ruleForm.totpCode,
    })
        .then(async (res) => {
            if (res?.code === 200 && res.data) {
                ElMessage({ message: '登录成功', type: 'success' })
                localStorage.setItem('userinfo', JSON.stringify(res.data))
                // 动态路由由路由守卫在导航时注册，这里直接跳转即可
                await router.push('/')
            } else {
                ElMessage.error(`登录失败！${res?.message || '用户信息获取失败'}`)
                loadCaptcha()
            }
        })
        .catch((error) => {
            // 1001=需要两步认证动态码：展开输入框而非报错
            if (error?.code === 1001) {
                showTotp.value = true
                ElMessage.warning(error.message)
                return
            }
            ElMessage.error(`登录失败！${error?.message} (${error?.code})`)
            loadCaptcha()
        })
        .finally(() => {
            loading.value = false
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

const ruleForm = reactive({
    username: '',
    password: '',
    captchaCode: '',
    totpCode: '',
})

// 两步认证：code=1001表示需要动态码
const showTotp = ref(false)

const rules = reactive({
    username: [{ validator: verifyUser.username, trigger: 'blur' }],
    password: [{ validator: verifyUser.password, trigger: 'blur' }],
    captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
    totpCode: [{ required: true, message: '请输入动态验证码', trigger: 'blur' }],
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

/**
 * 忘记密码弹窗
 */
const forgotVisible = ref(false)
const forgotFormRef = ref()
const forgotForm = reactive({
    email: '',
    code: '',
    newPassword: '',
})
const forgotRules = reactive({
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    ],
    code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
    newPassword: [{ validator: verifyUser.password, trigger: 'blur' }],
})
const forgotLoading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let countdownTimer = null

const openForgot = () => {
    forgotVisible.value = true
}

const sendCode = () => {
    if (!forgotForm.email) {
        ElMessage.warning('请先输入邮箱')
        return
    }
    codeSending.value = true
    sendResetCode({ email: forgotForm.email })
        .then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '验证码已发送，请查收邮件')
                codeCountdown.value = 60
                countdownTimer = setInterval(() => {
                    codeCountdown.value--
                    if (codeCountdown.value <= 0) {
                        clearInterval(countdownTimer)
                    }
                }, 1000)
            } else {
                ElMessage.error(`发送失败！${res.message}`)
            }
        })
        .catch((error) => {
            ElMessage.error(`发送失败！${error?.message}`)
        })
        .finally(() => {
            codeSending.value = false
        })
}

const submitForgot = () => {
    forgotFormRef.value.validate((valid) => {
        if (!valid) {
            return
        }
        forgotLoading.value = true
        resetPasswordByEmail({
            email: forgotForm.email,
            code: forgotForm.code,
            newPassword: forgotForm.newPassword,
        })
            .then((res) => {
                if (res?.code === 200) {
                    ElMessage.success(res.message || '密码重置成功，请使用新密码登录')
                    forgotVisible.value = false
                    resetForgotForm()
                } else {
                    ElMessage.error(`重置失败！${res.message}`)
                }
            })
            .catch((error) => {
                ElMessage.error(`重置失败！${error?.message}`)
            })
            .finally(() => {
                forgotLoading.value = false
            })
    })
}

const resetForgotForm = () => {
    forgotForm.email = ''
    forgotForm.code = ''
    forgotForm.newPassword = ''
}
</script>

<template>
    <el-form ref="ruleFormRef" :model="ruleForm" status-icon :rules="rules" label-width="auto" class="demo-ruleForm max-w-400px">
        <el-form-item prop="username">
            <el-input :prefix-icon="User" v-model="ruleForm.username" placeholder="请输入用户名" size="large" type="text" autocomplete="off" clearable />
        </el-form-item>
        <el-form-item prop="password">
            <el-input :prefix-icon="Lock" v-model="ruleForm.password" placeholder="请输入用密码" size="large" type="password" autocomplete="off" show-password />
        </el-form-item>
        <el-form-item v-if="policy.captchaEnabled" prop="captchaCode">
            <div class="flex gap-10px w-100%">
                <el-input v-model="ruleForm.captchaCode" placeholder="请输入验证码" size="large" type="text" autocomplete="off" @keyup.enter="submitForm(ruleFormRef)" />
                <img v-if="captcha.image" :src="captcha.image" title="看不清？点击刷新" class="captcha-img" alt="验证码" @click="loadCaptcha" />
            </div>
        </el-form-item>
        <el-form-item v-if="showTotp" prop="totpCode">
            <el-input v-model="ruleForm.totpCode" placeholder="请输入验证器6位动态码（或备用恢复码）" size="large" autocomplete="off" @keyup.enter="submitForm(ruleFormRef)">
                <template #prefix>
                    <el-icon><lock /></el-icon>
                </template>
            </el-input>
        </el-form-item>
        <el-form-item class="pointer">
            <el-button :loading="loading" class="submitForm" size="large" type="primary" @click="submitForm(ruleFormRef)"> 登 录 </el-button>
        </el-form-item>
        <div class="rest">
            <div @click="openForgot">忘记密码</div>
            <div @click="registration">注册账号</div>
        </div>
    </el-form>

    <el-dialog v-model="forgotVisible" title="找回密码" width="440" :close-on-click-modal="false" @closed="resetForgotForm">
        <el-form ref="forgotFormRef" :model="forgotForm" status-icon :rules="forgotRules" label-width="90px">
            <el-form-item label="邮箱" prop="email">
                <div class="flex gap-10px w-100%">
                    <el-input v-model="forgotForm.email" placeholder="请输入账号绑定的邮箱" />
                    <el-button :loading="codeSending" :disabled="codeCountdown > 0" @click="sendCode">
                        {{ codeCountdown > 0 ? `${codeCountdown}s后重发` : '发送验证码' }}
                    </el-button>
                </div>
            </el-form-item>
            <el-form-item label="验证码" prop="code">
                <el-input v-model="forgotForm.code" placeholder="请输入邮箱收到的验证码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="forgotForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="forgotVisible = false">取消</el-button>
            <el-button type="primary" :loading="forgotLoading" @click="submitForgot">重置密码</el-button>
        </template>
    </el-dialog>
</template>

<style scoped>
.submitForm {
    width: 100%;
    background-color: #2e5cf6;
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

.captcha-img {
    height: 40px;
    border-radius: 4px;
    cursor: pointer;
}

.rest {
    display: flex;
    justify-content: space-between;
    color: #2e5cf6;

    div {
        cursor: pointer;
    }
}
</style>
