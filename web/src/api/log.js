import service from '@/utils/request.js'

/**
 * 分页查询操作日志
 * @param data
 * @return {*}
 */
export function getOperateLogs(data) {
    return service({
        url: '/log/getOperateLogs',
        method: 'post',
        data,
    })
}
