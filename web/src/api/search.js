import service from '@/utils/request.js'

/**
 * 全局搜索：菜单/用户/角色/租户（登录即可）
 */
export function globalSearch(keyword) {
    return service({ url: '/search/all', method: 'get', params: { keyword } })
}
