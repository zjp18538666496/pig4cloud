import service from '@/utils/request.js'

/**
 * 下载文件
 * @param data
 * @return {*}
 */
export function file(data) {
    return service({
        url: `/file/download?filename=${data.filename}`,
        method: 'get',
    })
}
