import service from '@/utils/request.js'

export function getJobLists(data) {
    return service({ url: '/job/getJobLists', method: 'post', data })
}

export function createJob(data) {
    return service({ url: '/job/createJob', method: 'post', data })
}

export function updateJob(data) {
    return service({ url: '/job/updateJob', method: 'post', data })
}

export function delJob(data) {
    return service({ url: '/job/delJob', method: 'post', data })
}

export function runJobOnce(data) {
    return service({ url: '/job/runOnce', method: 'post', data })
}

/**
 * 任务执行日志（最近50条）
 */
export function getJobLogs(jobId) {
    return service({ url: '/job/getJobLogs', method: 'get', params: { jobId } })
}
