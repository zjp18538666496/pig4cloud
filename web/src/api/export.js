import service from '@/utils/request.js'

// 提交用户导出任务（异步，Excel后台生成）
export function submitUserExport(data) {
    return service({ url: '/export/submitUser', method: 'post', data })
}

// 导出任务分页（非超管仅本人任务）
export function getExportTasks(data) {
    return service({ url: '/export/getLists', method: 'post', data })
}

// 下载任务文件（blob）
export function downloadExportTask(id) {
    return service({
        url: `/export/download/${id}`,
        method: 'get',
        responseType: 'blob',
    })
}
