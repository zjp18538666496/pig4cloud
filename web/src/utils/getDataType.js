/**
 * 获取类型
 * @param target
 * @return {string}
 */
function getType(target) {
    // 使用正则表达式匹配并提取类型名称
    const typeStrMatch = Object.prototype.toString.call(target).match(/^\[object\s(.*)]$/)
    return typeStrMatch ? typeStrMatch[1].toLowerCase() : 'Unknown'
}

export { getType }
