<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { enable2fa, setup2fa } from '@/api/auth.js'

/**
 * 强制开启两步认证引导（login.2fa-force-enabled=true且未绑定时，登录后不可关闭）
 */
const props = defineProps({
    userInfo: { type: Object, required: true },
})
const emit = defineEmits(['enabled'])

const visible = ref(false)
const bind = reactive({ secret: '', qrImage: '', code: '', loading: false })

watch(
    () => props.userInfo.force2fa,
    (val) => {
        if (val) {
            visible.value = true
            loadQr()
        }
    },
    { immediate: true },
)

const loadQr = () => {
    setup2fa().then((res) => {
        if (res?.code === 200) {
            bind.secret = res.data.secret
            bind.qrImage = res.data.qrImage
        } else {
            ElMessage.error(`生成绑定二维码失败！${res?.message}`)
        }
    })
}

const confirm = () => {
    if (!bind.code || bind.code.length !== 6) {
        ElMessage.warning('请输入6位动态码')
        return
    }
    bind.loading = true
    enable2fa(bind.code)
        .then((res) => {
            if (res?.code === 200) {
                ElMessage.success('两步认证已开启')
                visible.value = false
                emit('enabled')
            } else {
                ElMessage.error(`绑定失败！${res?.message}`)
            }
        })
        .finally(() => {
            bind.loading = false
        })
}
</script>

<template>
    <el-dialog
        v-model="visible"
        title="请开启两步认证"
        width="420"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
    >
        <el-alert type="warning" :closable="false" title="系统已开启强制两步认证，请使用验证器App扫码绑定后继续使用" class="mb-12px" />
        <div class="text-center">
            <img v-if="bind.qrImage" :src="bind.qrImage" class="w-180px" alt="绑定二维码" />
        </div>
        <el-form label-width="80px" class="mt-10px">
            <el-form-item label="动态码">
                <el-input v-model="bind.code" placeholder="输入App上的6位动态码" maxlength="6" @keyup.enter="confirm" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button type="primary" :loading="bind.loading" @click="confirm">确认绑定</el-button>
        </template>
    </el-dialog>
</template>
