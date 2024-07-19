<template>
    <div>
        <el-upload
            class="upload-demo"
            :action="uploadUrl"
            :data="extraData"
            :headers="{ authorization }"
            :on-success="handleSuccess"
            :on-error="handleError"
            :before-upload="beforeUpload"
            :file-list="fileList"
            :name="fileParamName"
            :multiple='true'
        >
            <el-button size="small" type="primary">点击上传</el-button>
          <img src="http://127.0.0.1:9000/file/FTP/download1?filename=/test/11.jpg" alt="66">
        </el-upload>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { file } from '@/api/file.js'

const uploadUrl = import.meta.env.VITE_BASE_URL + '/file/uploadFile' // 后端上传接口地址
let extraData = ref({ imgName: '' }) // 额外参数，这里包含文件名
let fileList = ref([]) // 文件列表
const authorization = localStorage.getItem('authorization')
const fileParamName = ref('files')
const beforeUpload = (file) => {
    // 在文件上传之前设置额外参数
    // extraData.imgName = file.name.split('.')[0] // 获取文件名，不包括扩展名
    // 可以在这里添加文件类型和大小的校验
}
const handleSuccess = (response, file, fileList) => {
    // 文件上传成功时的钩子函数
    this.$message.success(response) // 显示后端返回的成功信息
}
const handleError = (err, file, fileList) => {
    // 文件上传失败时的钩子函数
    this.$message.error('上传失败，请重试！')
}
</script>

<style>
/* 样式根据需要进行添加 */
</style>