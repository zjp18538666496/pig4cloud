import service from '@/utils/request.js'

// 提交申请（登录即可）
export function applyApproval(data) {
    return service({ url: '/approval/apply', method: 'post', data })
}

// 我的申请
export function myApplications(params) {
    return service({ url: '/approval/myApplications', method: 'get', params })
}

// 可申请角色选项
export function roleOptions() {
    return service({ url: '/approval/roleOptions', method: 'get' })
}

// 审批中心列表（需approval:manage）
export function getApprovals(data) {
    return service({ url: '/approval/getLists', method: 'post', data })
}

// 审批（通过/驳回）
export function approve(data) {
    return service({ url: '/approval/approve', method: 'post', data })
}
