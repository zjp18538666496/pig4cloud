<template>
    <div class="header">
        <!-- 代理登录横幅 -->
        <div v-if="userInfo.impersonator" class="proxy-banner">
            <span>代理视角：{{ userInfo.username }}（由 {{ userInfo.impersonator }} 发起）</span>
            <el-button size="small" type="warning" plain @click="exitProxy">退出代理</el-button>
        </div>
        <GlobalSearch />
        <div class="flex items-center gap-12px mr-12px">
            <!-- 消息中心铃铛 -->
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
                <el-icon class="action-icon" :size="18" @click="openMessages">
                    <bell />
                </el-icon>
            </el-badge>
            <!-- 暗黑模式切换 -->
            <el-tooltip :content="$t('header.theme')" placement="bottom">
                <el-icon class="action-icon" :size="18" @click="switchDark">
                    <moon v-if="!dark" />
                    <sunny v-else />
                </el-icon>
            </el-tooltip>
            <!-- 语言切换 -->
            <el-tooltip :content="$t('header.language')" placement="bottom">
                <span class="locale-text" @click="switchLocale">{{ locale === 'zh-CN' ? 'EN' : '中' }}</span>
            </el-tooltip>
        </div>
        <div class="flex justify-center items-center">
            <el-avatar class="mr8px" :size="30" shape="circle" :src="avatarUrl" />
            <el-dropdown>
                <span>
                    {{ userInfo.name }}
                    <el-icon>
                        <arrow-down />
                    </el-icon>
                </span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item v-for="(item, i) in routerList" :divided="item.divided" :key="i" @click="goto(item.src)">
                            {{ item.name }}
                        </el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
    <el-drawer v-model="drawer" title="个人信息">
        <presonalCenter />
    </el-drawer>

    <!-- 强制开启两步认证（不可关闭） -->
    <Force2faDialog :user-info="userInfo" @enabled="on2faEnabled" />

    <!-- 消息中心抽屉 -->
    <el-drawer v-model="messageDrawer" :title="$t('header.messages')" size="480">
        <div class="flex justify-between items-center mb-10px">
            <el-button size="small" @click="markAll" v-if="messages.length">{{ $t('header.markAllRead') }}</el-button>
            <template v-if="canSend">
                <el-button size="small" type="primary" plain @click="sendVisible = true">发消息</el-button>
            </template>
        </div>
        <el-empty v-if="!messages.length" :description="$t('header.noMessage')" :image-size="80" />
        <div v-else class="message-list">
            <div
                v-for="message in messages"
                :key="message.id"
                class="message-item"
                :class="{ unread: message.read_flag === '0' }"
                @click="readMessage(message)"
            >
                <div class="flex justify-between items-center">
                    <span class="message-title">
                        <el-badge is-dot :hidden="message.read_flag === '1'" class="mr-6px">{{ message.title }}</el-badge>
                    </span>
                    <span class="message-time">{{ message.create_time }}</span>
                </div>
                <div class="message-content">{{ message.content }}</div>
                <div class="message-from">来自：{{ message.create_by || '系统' }}</div>
            </div>
        </div>
        <div class="flex justify-end mt-10px" v-if="messageTotal > messageQuery.pageSize">
            <el-pagination
                small
                background
                layout="prev, pager, next"
                :total="messageTotal"
                :page-size="messageQuery.pageSize"
                v-model:current-page="messageQuery.page"
                @current-change="loadMessages"
            />
        </div>
    </el-drawer>

    <!-- 发送站内信 -->
    <el-dialog v-model="sendVisible" title="发送站内信" width="440" :close-on-click-modal="false">
        <el-form :model="sendForm" label-width="90px">
            <el-form-item :label="$t('header.sendTo')">
                <el-input v-model="sendForm.target_username" :placeholder="$t('header.sendToPlaceholder')" />
            </el-form-item>
            <el-form-item label="标题">
                <el-input v-model="sendForm.title" maxlength="128" />
            </el-form-item>
            <el-form-item label="内容">
                <el-input v-model="sendForm.content" type="textarea" :rows="3" maxlength="1000" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="sendVisible = false">{{ $t('common.cancel') }}</el-button>
            <el-button type="primary" :loading="sending" @click="doSend">{{ $t('header.send') }}</el-button>
        </template>
    </el-dialog>

    <!-- 强制修改初始密码（不可关闭） -->
    <el-dialog
        v-model="forceChangeVisible"
        :title="$t('header.forceChangeTitle')"
        width="440"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
    >
        <el-alert :title="$t('header.forceChangeTip')" type="warning" :closable="false" class="mb-16px" />
        <el-form ref="forceFormRef" :model="forceForm" :rules="forceRules" label-width="90px">
            <el-form-item :label="$t('header.oldPassword')" prop="password">
                <el-input v-model="forceForm.password" type="password" show-password />
            </el-form-item>
            <el-form-item :label="$t('header.newPassword')" prop="newPassword">
                <el-input v-model="forceForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item :label="$t('header.confirmPassword')" prop="confirmPassword">
                <el-input v-model="forceForm.confirmPassword" type="password" show-password />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button type="primary" :loading="forceLoading" @click="doForceChange">{{ $t('common.confirm') }}</el-button>
        </template>
    </el-dialog>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ArrowDown, Bell, Moon, Sunny } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import presonalCenter from '@/views/personal-center/Index.vue'
import GlobalSearch from './GlobalSearch.vue'
import Force2faDialog from './Force2faDialog.vue'
import { logout as logoutApi } from '@/api/auth.js'
import { getMyMessages, getUnreadCount, markAllRead, markRead, sendMessage } from '@/api/message.js'
import { updatePassword } from '@/api/user.js'
import { toggleDark, isDark } from '@/utils/dark.js'
import { setLocale } from '@/locales'
import { VerifyUser } from '@/utils/vali.js'

const { t, locale } = useI18n()
const baseUrl = import.meta.env.VITE_BASE_URL
const drawer = ref(false)
const router = useRouter()

// 安全解析，存储缺失/损坏时兜底空对象，避免渲染崩溃
const parseUserInfo = () => {
    try {
        return JSON.parse(localStorage.getItem('userinfo')) || {}
    } catch {
        return {}
    }
}
const userInfo = ref(parseUserInfo())
// 头像走公开接口（仅返回登记为头像的FTP路径）
const avatarUrl = computed(() => (userInfo.value.id ? baseUrl + 'file/avatar/' + userInfo.value.id : ''))

const permissions = computed(() => userInfo.value.permissions || [])
const canSend = computed(() => permissions.value.includes('notice:write'))

const routerList = computed(() => [
    { src: '/', name: t('header.home'), divided: false },
    { src: '/personalCenter', name: t('header.profile'), divided: false },
    { src: '/login', name: t('header.logout'), divided: true },
])

// 退出登录：先通知服务端拉黑token销毁会话，再清空本地缓存
const logout = (src) => {
    ElMessageBox.confirm(t('header.logoutConfirm'), t('common.confirm'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
    })
        .then(() => {
            logoutApi().finally(() => {
                router.push(src)
                ElMessage({ message: t('header.logoutSuccess'), type: 'success' })
                localStorage.clear()
            })
        })
        .catch(() => {})
}

const goto = (src) => {
    if (src === '/login') {
        return logout(src)
    }
    if (src === '/personalCenter') {
        drawer.value = true
        return
    }
    router.push(src)
}

/**
 * 暗黑模式/语言切换
 */
const dark = ref(isDark())
const switchDark = () => {
    dark.value = toggleDark()
}
const switchLocale = () => {
    setLocale(locale.value === 'zh-CN' ? 'en' : 'zh-CN')
}

/**
 * 强制2FA绑定成功：更新本地用户标记
 */
const on2faEnabled = () => {
    userInfo.value = { ...userInfo.value, force2fa: false }
    localStorage.setItem('userinfo', JSON.stringify(userInfo.value))
}

/**
 * 退出代理：还原发起代理前的登录态
 */
const exitProxy = () => {
    try {
        const backup = JSON.parse(localStorage.getItem('proxyBackup') || 'null')
        if (backup && backup.authorization) {
            localStorage.setItem('authorization', backup.authorization)
            localStorage.setItem('refreshToken', backup.refreshToken)
            localStorage.setItem('userinfo', backup.userinfo)
        }
        localStorage.removeItem('proxyBackup')
        window.location.href = '/'
    } catch {
        localStorage.clear()
        window.location.href = '/login'
    }
}

/**
 * 消息中心：未读数轮询 + WebSocket实时推送刷新
 */
const unreadCount = ref(0)
const messageDrawer = ref(false)
const messages = ref([])
const messageTotal = ref(0)
const messageQuery = reactive({ page: 1, pageSize: 10 })
const sendVisible = ref(false)
const sending = ref(false)
const sendForm = reactive({ target_username: '', title: '', content: '' })
let ws = null
let pollTimer = null

const refreshUnread = () => {
    getUnreadCount().then((res) => {
        if (res?.code === 200) {
            unreadCount.value = Number(res.data) || 0
        }
    })
}

const loadMessages = () => {
    getMyMessages(messageQuery).then((res) => {
        if (res?.code === 200) {
            messages.value = res.data.rows
            messageTotal.value = res.data.total
        }
    })
}

const openMessages = () => {
    messageDrawer.value = true
    loadMessages()
    refreshUnread()
}

const readMessage = (message) => {
    if (message.read_flag === '1') {
        return
    }
    markRead(message.id).then((res) => {
        if (res?.code === 200) {
            message.read_flag = '1'
            refreshUnread()
        }
    })
}

const markAll = () => {
    markAllRead().then((res) => {
        if (res?.code === 200) {
            messages.value.forEach((message) => (message.read_flag = '1'))
            refreshUnread()
        }
    })
}

const doSend = () => {
    if (!sendForm.title) {
        ElMessage.warning('请输入消息标题')
        return
    }
    sending.value = true
    sendMessage({ ...sendForm })
        .then((res) => {
            if (res?.code === 200) {
                ElMessage.success(res.message || '发送成功')
                sendVisible.value = false
                sendForm.title = ''
                sendForm.content = ''
            } else {
                ElMessage.error(`发送失败！${res?.message}`)
            }
        })
        .finally(() => {
            sending.value = false
        })
}

/**
 * WebSocket实时通知（token放在路径上，后端握手校验）
 */
const connectWs = () => {
    const token = (localStorage.getItem('authorization') || '').replace('Bearer ', '')
    if (!token) {
        return
    }
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    try {
        ws = new WebSocket(`${protocol}//${location.host}/ws/${token}`)
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data)
                if (data.type === 'message') {
                    refreshUnread()
                    if (messageDrawer.value) {
                        loadMessages()
                    }
                }
            } catch {
                // 忽略非JSON消息
            }
        }
        ws.onclose = () => {
            // 断线30秒后重连（页面存活期间）
            setTimeout(() => {
                if (localStorage.getItem('authorization')) {
                    connectWs()
                }
            }, 30000)
        }
    } catch {
        // WebSocket不可用时仅靠轮询
    }
}

/**
 * 强制修改初始密码（初始/重置密码未改或密码过期）
 */
const verifyUser = new VerifyUser()
const forceChangeVisible = ref(false)
const forceLoading = ref(false)
const forceFormRef = ref()
const forceForm = reactive({ password: '', newPassword: '', confirmPassword: '' })
const forceRules = reactive({
    password: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [{ validator: verifyUser.newPassword, trigger: 'blur' }],
    confirmPassword: [{ validator: verifyUser.confirmPassword, trigger: 'blur' }],
})
const checkForceChange = () => {
    forceChangeVisible.value = !!userInfo.value.forcePwdChange
}
const doForceChange = () => {
    forceFormRef.value.validate((valid) => {
        if (!valid) return
        forceLoading.value = true
        updatePassword({
            password: forceForm.password,
            newPassword: forceForm.newPassword,
            confirmPassword: forceForm.confirmPassword,
        })
            .then((res) => {
                if (res?.code === 200) {
                    ElMessage.success('密码修改成功')
                    forceChangeVisible.value = false
                    // 更新本地用户信息标记
                    userInfo.value = { ...userInfo.value, forcePwdChange: false }
                    localStorage.setItem('userinfo', JSON.stringify(userInfo.value))
                } else {
                    ElMessage.error(`修改失败！${res?.message}`)
                }
            })
            .finally(() => {
                forceLoading.value = false
            })
    })
}

onMounted(() => {
    refreshUnread()
    checkForceChange()
    connectWs()
    pollTimer = setInterval(refreshUnread, 60 * 1000)
})

onUnmounted(() => {
    if (pollTimer) {
        clearInterval(pollTimer)
    }
    if (ws) {
        ws.close()
    }
})
</script>

<style scoped>
.header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    height: 50px;
}

.el-dropdown {
    color: #fff;
    cursor: pointer;
}

.action-icon {
    color: #fff;
    cursor: pointer;
    display: flex;
}

.locale-text {
    font-size: 13px;
    color: #fff;
    cursor: pointer;
}

.proxy-banner {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 2px 12px;
    border-radius: 12px;
    background: rgba(255, 143, 31, 0.25);
    color: #fff;
    font-size: 12px;
}

:focus-visible {
    outline: none;
}

.message-list {
    max-height: calc(100vh - 220px);
    overflow: auto;
}

.message-item {
    padding: 10px;
    border-bottom: 1px dashed #e4e7ed;
    cursor: pointer;
}

.message-item.unread {
    background: rgba(46, 92, 246, 0.04);
}

.message-title {
    font-weight: 600;
    font-size: 14px;
}

.message-time {
    color: #909399;
    font-size: 12px;
}

.message-content {
    margin-top: 4px;
    color: #606266;
    font-size: 13px;
    white-space: pre-wrap;
}

.message-from {
    margin-top: 4px;
    color: #c0c4cc;
    font-size: 12px;
}

html.dark .message-item.unread {
    background: rgba(46, 92, 246, 0.15);
}
</style>
