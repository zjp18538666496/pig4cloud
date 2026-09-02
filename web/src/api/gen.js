import service from '@/utils/request.js'

/**
 * 可生成代码的表列表
 */
export function getGenTables() {
    return service({ url: '/gen/tables', method: 'get' })
}

/**
 * 代码预览：{文件路径: 代码内容}
 */
export function previewGen(data) {
    return service({ url: '/gen/preview', method: 'post', data })
}

/**
 * 打包下载生成代码（zip blob）
 */
export function downloadGen(data) {
    return service({ url: '/gen/download', method: 'post', data, responseType: 'blob' })
}
