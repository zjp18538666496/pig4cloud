const KEY = 'theme-dark'

export const isDark = () => localStorage.getItem(KEY) === '1'

/**
 * 应用暗黑模式（element-plus dark css-vars 按 html.dark 生效）
 */
export const applyDark = () => {
    document.documentElement.classList.toggle('dark', isDark())
}

export const toggleDark = () => {
    localStorage.setItem(KEY, isDark() ? '0' : '1')
    applyDark()
    return isDark()
}
