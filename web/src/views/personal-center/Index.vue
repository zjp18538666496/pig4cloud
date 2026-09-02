<template>
    <el-tabs v-model="activeName" class="demo-tabs">
        <el-tab-pane label="基本信息" name="basic">
            <el-form :model="form" label-width="auto" style="max-width: 600px">
                <el-form-item label="  ">
                    <!-- 头像走公开接口（仅返回登记为头像的FTP路径） -->
                    <el-avatar :size="150" shape="circle" :src="baseUrl + 'file/avatar/' + (form.id || '')" />
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

            <el-divider content-position="left">两步认证（2FA）</el-divider>
            <div class="twofa-card">
                <div class="flex justify-between items-center">
                    <div>
                        <div>验证器动态码二次验证（Google Authenticator/腾讯身份验证器等）</div>
                        <el-tag :type="twoFa.enabled ? 'success' : 'info'" class="mt-8px">
                            {{ twoFa.enabled ? '已开启' : '未开启' }}
                        </el-tag>
                    </div>
                    <el-button v-if="!twoFa.enabled" type="primary" @click="startBind2fa">开启</el-button>
                    <template v-else>
                        <el-button type="warning" plain @click="regenVisible = true">重新生成备用码</el-button>
                        <el-button type="danger" plain @click="unbindVisible = true">解绑</el-button>
                    </template>
                </div>
            </div>
        </el-tab-pane>
        <el-tab-pane label="登录与设备" name="devices">
            <h4 class="section-title">我的在线设备</h4>
            <el-table :data="devices" border size="small">
                <el-table-column label="设备" min-width="110">
                    <template #default="scope">{{ scope.row.browser }}</template>
                </el-table-column>
                <el-table-column prop="ip" label="IP" width="130" />
                <el-table-column prop="loginTime" label="登录时间" width="160" />
                <el-table-column prop="lastAccessTime" label="最后活跃" width="160" />
                <el-table-column label="操作" width="110" align="center">
                    <template #default="scope">
                        <el-tag v-if="scope.row.current" type="success" size="small">当前设备</el-tag>
                        <el-button v-else size="small" type="danger" link @click="kickDevice(scope.row)">下线</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <h4 class="section-title">登录历史</h4>
            <el-table :data="loginLogs" border size="small">
                <el-table-column prop="createTime" label="时间" width="160" />
                <el-table-column prop="ip" label="IP" width="130" />
                <el-table-column label="结果" width="80" align="center">
                    <template #default="scope">
                        <el-tag :type="scope.row.success ? 'success' : 'danger'" size="small">
                            {{ scope.row.success ? '成功' : '失败' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="message" label="说明" show-overflow-tooltip />
            </el-table>

            <h4 class="section-title">我的操作记录</h4>
            <el-table :data="operateLogs" border size="small">
                <el-table-column prop="createTime" label="时间" width="160" />
                <el-table-column prop="module" label="模块" width="90" />
                <el-table-column prop="operation" label="操作" width="100" />
                <el-table-column prop="url" label="接口" show-overflow-tooltip />
                <el-table-column label="结果" width="70" align="center">
                    <template #default="scope">
                        <el-tag :type="scope.row.success ? 'success' : 'danger'" size="small">
                            {{ scope.row.success ? '成功' : '失败' }}
                        </el-tag>
                    </template>
                </el-table-column>
            </el-table>
        </el-tab-pane>
    </el-tabs>

    <!-- 2FA绑定：扫码+首次动态码 -->
    <el-dialog v-model="bindVisible" title="开启两步认证" width="400" :close-on-click-modal="false" @closed="bindVisible = false">
        <div class="text-center">
            <img v-if="bind.qrImage" :src="bind.qrImage" class="w-200px" alt="绑定二维码" />
            <div class="text-12px color-#909399 mt-8px">
                使用验证器App扫码添加，无法扫码时可手动输入密钥：
            </div>
            <el-text class="break-all" type="primary" size="small">{{ bind.secret }}</el-text>
        </div>
        <el-form label-width="90px" class="mt-12px">
            <el-form-item label="动态码">
                <el-input v-model="bind.code" placeholder="输入App上的6位动态码" maxlength="6" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="bindVisible = false">取消</el-button>
            <el-button type="primary" :loading="bind.loading" @click="confirmBind">确认绑定</el-button>
        </template>
    </el-dialog>

    <!-- 备用恢复码（仅展示一次） -->
    <el-dialog v-model="backupVisible" title="备用恢复码（仅显示一次）" width="400" :close-on-click-modal="false">
        <el-alert type="warning" :closable="false" title="手机丢失时可用备用码登录，每个仅可用一次。请截图或抄写保存！" class="mb-12px" />
        <div class="backup-codes">
            <el-tag v-for="code in backupCodes" :key="code" size="large" class="backup-code">{{ code }}</el-tag>
        </div>
        <template #footer>
            <el-button type="primary" @click="backupVisible = false">我已保存</el-button>
        </template>
    </el-dialog>

    <!-- 重新生成备用码 -->
    <el-dialog v-model="regenVisible" title="重新生成备用恢复码" width="380" :close-on-click-modal="false">
        <el-alert type="warning" :closable="false" title="旧备用码将全部作废，请输入当前动态码确认" class="mb-12px" />
        <el-input v-model="regen.code" placeholder="验证器6位动态码" maxlength="6" />
        <template #footer>
            <el-button @click="regenVisible = false">取消</el-button>
            <el-button type="primary" :loading="regen.loading" @click="confirmRegen">确认重新生成</el-button>
        </template>
    </el-dialog>

    <!-- 2FA解绑：密码+动态码 -->
    <el-dialog v-model="unbindVisible" title="解绑两步认证" width="400" :close-on-click-modal="false">
        <el-form label-width="90px">
            <el-form-item label="登录密码">
                <el-input v-model="unbind.password" type="password" show-password />
            </el-form-item>
            <el-form-item label="动态码">
                <el-input v-model="unbind.code" placeholder="验证器动态码或备用恢复码" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="unbindVisible = false">取消</el-button>
            <el-button type="danger" :loading="unbind.loading" @click="confirmUnbind">确认解绑</el-button>
        </template>
    </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { delUser, updatePassword, updateUser } from '@/api/user.js'
import { disable2fa, enable2fa, get2faStatus, regenerateBackupCodes, setup2fa } from '@/api/auth.js'
import { getMyLoginLogs, getMyOperateLogs, getMySessions, kickMySession } from '@/api/profile.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VerifyUser } from '@/utils/vali.js'
const verifyUser = new VerifyUser()
const baseUrl = import.meta.env.VITE_BASE_URL
const router = useRouter()
const activeName = ref('basic')
const formEl = ref()
let loading = ref(false)
// 安全解析，存储缺失/损坏时兜底空对象
const form = ref((() => {
    try {
        return JSON.parse(localStorage.getItem('userinfo')) || {}
    } catch {
        return {}
    }
})())
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
        .then(() => delUser({ username: form.value.username }))
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

/**
 * 两步认证（2FA）
 */
const twoFa = reactive({ enabled: false })
const bindVisible = ref(false)
const bind = reactive({ secret: '', qrImage: '', code: '', loading: false })
const backupVisible = ref(false)
const backupCodes = ref([])
const unbindVisible = ref(false)
const unbind = reactive({ password: '', code: '', loading: false })

const refresh2faState = () => {
    // 以本地用户信息+会话探测的方式粗略同步状态：登录响应未带2FA标记，这里通过解绑接口的可用性由服务端兜底
    twoFa.enabled = false
}
refresh2faState()

const startBind2fa = () => {
    setup2fa().then((res) => {
        if (res?.code === 200) {
            bind.secret = res.data.secret
            bind.qrImage = res.data.qrImage
            bind.code = ''
            bindVisible.value = true
        } else {
            ElMessage.error(`生成失败！${res?.message}`)
        }
    })
}

const confirmBind = () => {
    if (!bind.code || bind.code.length !== 6) {
        ElMessage.warning('请输入6位动态码')
        return
    }
    bind.loading = true
    enable2fa(bind.code)
        .then((res) => {
            if (res?.code === 200) {
                twoFa.enabled = true
                backupCodes.value = res.data || []
                bindVisible.value = false
                backupVisible.value = true
            } else {
                ElMessage.error(`绑定失败！${res?.message}`)
            }
        })
        .finally(() => {
            bind.loading = false
        })
}

const confirmUnbind = () => {
    unbind.loading = true
    disable2fa({ password: unbind.password, code: unbind.code })
        .then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '已解绑')
                unbindVisible.value = false
                unbind.password = ''
                unbind.code = ''
                twoFa.enabled = false
            } else {
                ElMessage.error(`解绑失败！${res?.message}`)
            }
        })
        .finally(() => {
            unbind.loading = false
        })
}

/**
 * 重新生成备用恢复码（旧码全部作废，需验证当前动态码）
 */
const regenVisible = ref(false)
const regen = reactive({ code: '', loading: false })
const confirmRegen = () => {
    if (!regen.code) {
        ElMessage.warning('请输入当前动态码')
        return
    }
    regen.loading = true
    regenerateBackupCodes(regen.code)
        .then((res) => {
            if (res?.code === 200) {
                regenVisible.value = false
                regen.code = ''
                backupCodes.value = res.data || []
                backupVisible.value = true
            } else {
                ElMessage.error(`生成失败！${res?.message}`)
            }
        })
        .finally(() => {
            regen.loading = false
        })
}

/**
 * 登录与设备
 */
const devices = ref([])
const loginLogs = ref([])
const operateLogs = ref([])

const loadProfileData = () => {
    get2faStatus().then((res) => {
        if (res?.code === 200) {
            twoFa.enabled = !!res.data?.enabled
        }
    })
    getMySessions().then((res) => {
        if (res?.code === 200) {
            devices.value = res.data
        }
    })
    getMyLoginLogs({ page: 1, pageSize: 10 }).then((res) => {
        if (res?.code === 200) loginLogs.value = res.data.rows
    })
    getMyOperateLogs({ page: 1, pageSize: 10 }).then((res) => {
        if (res?.code === 200) operateLogs.value = res.data.rows
    })
}

const kickDevice = (row) => {
    kickMySession({ tokenJti: row.tokenJti }).then((res) => {
        if (res?.code === 200) {
            ElMessage.success(res.message || '已下线')
            loadProfileData()
        } else {
            ElMessage.error(`操作失败！${res?.message}`)
        }
    })
}

onMounted(loadProfileData)
</script>

<style scoped>
.section-title {
    margin: 18px 0 8px;
    font-size: 14px;
    font-weight: 600;
}

.twofa-card {
    max-width: 600px;
    padding: 12px 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
}

.backup-codes {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
}

.backup-code {
    justify-content: center;
    font-family: monospace;
}
</style>
