import service from '@/utils/request.js'

/**
 * 分页查询字典
 */
export function getDictLists(data) {
    return service({ url: '/dict/getDictLists', method: 'post', data })
}

export function createDict(data) {
    return service({ url: '/dict/createDict', method: 'post', data })
}

export function updateDict(data) {
    return service({ url: '/dict/updateDict', method: 'post', data })
}

export function delDict(data) {
    return service({ url: '/dict/delDict', method: 'post', data })
}

/**
 * 字典项列表
 */
export function getDictItems(dictId) {
    return service({ url: '/dict/getItemLists', method: 'post', data: { dict_id: dictId } })
}

/**
 * 全量保存字典项
 */
export function saveDictItems(dictId, items) {
    return service({ url: '/dict/saveItems', method: 'post', data: { dict_id: dictId, items } })
}

/**
 * 按编码取启用字典项（业务下拉数据源）
 */
export function getDictByCode(code) {
    return service({ url: '/dict/getDictByCode', method: 'get', params: { code } })
}
