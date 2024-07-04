/**
 * 防抖函数
 * @param func 函数
 * @param wait 延迟时间
 * @return {(function(...[*]): void)|*}
 */
export function debounce(func, wait) {
    let timeout;
    return function(...args) {
        const context = this;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
}

/**
 * 节流函数
 * @param func 函数
 * @param limit 延迟时间
 * @return {(function(...[*]): void)|*}
 */
export function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}