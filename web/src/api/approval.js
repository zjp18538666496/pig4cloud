import service from '@/utils/request.js'

/**
 * 提交角色申请
 */
export function applyApproval(data) {
    return service({ url: '/approval/apply', method: 'post', data })
}

/**
 * 我的申请
 */
export function getMyApprovals() {
    return service({ url: '/approval/my', method: 'get' })
}

/**
 * 我的待办审批
 */
export function getPendingApprovals() {
    return service({ url: '/approval/pending', method: 'get' })
}

/**
 * 通过/驳回
 */
export function completeApproval(data) {
    return service({ url: '/approval/complete', method: 'post', data })
}
