<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadGen, getGenTables, previewGen } from '@/api/gen.js'

/**
 * 代码生成器：选表 → 预览各文件代码 → zip下载
 */
const tables = ref([])
const loading = ref(false)
const form = reactive({
    table: '',
    module: '',
    className: '',
    author: 'gen',
})
// 预览结果：{文件路径: 代码}
const files = ref({})
const activeFile = ref('')

onMounted(() => {
    getGenTables().then((res) => {
        if (res?.code === 200) {
            tables.value = res.data
        } else {
            ElMessage.error(`获取表列表失败！${res?.message}`)
        }
    })
})

const doPreview = () => {
    if (!form.table) {
        ElMessage.warning('请选择表')
        return
    }
    loading.value = true
    previewGen(form)
        .then((res) => {
            if (res?.code === 200) {
                files.value = res.data
                activeFile.value = Object.keys(res.data)[0]
            } else {
                ElMessage.error(`生成失败！${res?.message}`)
            }
        })
        .finally(() => {
            loading.value = false
        })
}

const doDownload = () => {
    downloadGen(form).then((res) => {
        if (res instanceof Blob) {
            const url = URL.createObjectURL(res)
            const link = document.createElement('a')
            link.href = url
            link.download = `${form.table}-code.zip`
            link.click()
            URL.revokeObjectURL(url)
        } else {
            ElMessage.error('下载失败，请先预览生成')
        }
    })
}
</script>

<template>
    <div class="page">
        <el-card shadow="never" class="mb-16px">
            <div class="flex flex-wrap items-end gap-10px">
                <div>
                    <div class="text-12px color-#909399 mb-4px">数据表</div>
                    <el-select v-model="form.table" filterable placeholder="选择要生成代码的表" class="w-240px">
                        <el-option v-for="t in tables" :key="t.table_name" :value="t.table_name"
                                   :label="`${t.table_name} ${t.table_comment || ''}`" />
                    </el-select>
                </div>
                <div>
                    <div class="text-12px color-#909399 mb-4px">业务包module</div>
                    <el-input v-model="form.module" placeholder="默认取表名" class="w-160px" />
                </div>
                <div>
                    <div class="text-12px color-#909399 mb-4px">类名</div>
                    <el-input v-model="form.className" placeholder="默认按表名驼峰" class="w-180px" />
                </div>
                <div>
                    <div class="text-12px color-#909399 mb-4px">作者</div>
                    <el-input v-model="form.author" class="w-120px" />
                </div>
                <el-button type="primary" :loading="loading" @click="doPreview">生成预览</el-button>
                <el-button type="success" :disabled="!Object.keys(files).length" @click="doDownload">打包下载zip</el-button>
            </div>
            <el-alert class="mt-10px" type="info" :closable="false"
                      title="生成内容：后端Entity/Mapper/Service/Controller + 前端api.js/列表页 + 菜单SQL。代码按本项目规范生成，请检查后放入对应目录并按menu.sql配置菜单。" />
        </el-card>

        <el-card v-if="Object.keys(files).length" shadow="never">
            <el-tabs v-model="activeFile">
                <el-tab-pane v-for="(code, path) in files" :key="path" :label="path.split('/').pop()" :name="path">
                    <div class="text-12px color-#909399 mb-6px">{{ path }}</div>
                    <pre class="code-block">{{ code }}</pre>
                </el-tab-pane>
            </el-tabs>
        </el-card>
    </div>
</template>

<style scoped>
.page {
    padding: 20px;
}

.code-block {
    max-height: 480px;
    overflow: auto;
    background: #1e1e1e;
    color: #d4d4d4;
    padding: 14px;
    border-radius: 6px;
    font-family: monospace;
    font-size: 12px;
    line-height: 1.5;
}

html.dark .code-block {
    background: #111;
}
</style>
