<script setup>
import '@wangeditor/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

/**
 * 公告富文本编辑器（wangEditor Vue3）：图片/标题/表格/样式，v-model绑定HTML
 */
const props = defineProps({
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '请输入公告内容，支持图文排版' },
})
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const valueRef = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
    if (val !== valueRef.value) valueRef.value = val
})
watch(valueRef, (val) => emit('update:modelValue', val))

const toolbarConfig = { excludeKeys: ['group-video', 'insertVideo', 'uploadVideo', 'fullScreen'] }
const editorConfig = { placeholder: props.placeholder, MENU_CONF: {} }

const handleCreated = (editor) => {
    editorRef.value = editor
}
onBeforeUnmount(() => {
    editorRef.value?.destroy()
})
</script>

<template>
    <div class="rich-editor">
        <Toolbar class="toolbar" :editor="editorRef" :default-config="toolbarConfig" :mode="'default'" />
        <Editor class="body" v-model="valueRef" :default-config="editorConfig" :mode="'default'"
            @on-created="handleCreated" />
    </div>
</template>

<style scoped>
.rich-editor {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    z-index: 100;
    width: 100%;
}

.toolbar {
    border-bottom: 1px solid #eee;
}

.body {
    height: 280px;
    overflow-y: hidden;
}
</style>
