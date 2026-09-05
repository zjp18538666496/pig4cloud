import { isDark } from './dark.js'

/**
 * ECharts深色模式适配：按html.dark状态返回坐标轴/文字/提示框配色，
 * 并提供主题切换监听（MutationObserver观察html.class），回调里重新setOption即可
 */
export function chartTheme() {
    const dark = isDark()
    return {
        dark,
        textColor: dark ? '#cbd5e1' : '#606266',
        axisLine: dark ? '#475569' : '#dcdfe6',
        splitLine: dark ? '#334155' : '#e5e7eb',
        tooltipBg: dark ? '#1e293b' : '#ffffff',
        tooltipBorder: dark ? '#475569' : '#e4e7ed',
    }
}

/**
 * 应用主题配色到option（就地补齐textStyle/axis线与分割线/tooltip背景）
 */
export function applyChartTheme(option) {
    const t = chartTheme()
    option.textColor = t.textColor
    option.tooltip = Object.assign({}, option.tooltip, {
        backgroundColor: t.tooltipBg,
        borderColor: t.tooltipBorder,
        textStyle: Object.assign({}, option.tooltip?.textStyle, { color: t.textColor }),
    })
    if (option.xAxis) {
        const axes = Array.isArray(option.xAxis) ? option.xAxis : [option.xAxis]
        axes.forEach((axis) => {
            axis.axisLine = Object.assign({}, axis.axisLine, { lineStyle: { color: t.axisLine } })
            axis.axisLabel = Object.assign({}, axis.axisLabel, { color: t.textColor })
        })
    }
    if (option.yAxis) {
        const axes = Array.isArray(option.yAxis) ? option.yAxis : [option.yAxis]
        axes.forEach((axis) => {
            axis.axisLine = Object.assign({}, axis.axisLine, { lineStyle: { color: t.axisLine } })
            axis.axisLabel = Object.assign({}, axis.axisLabel, { color: t.textColor })
            axis.splitLine = Object.assign({}, axis.splitLine, { lineStyle: { color: t.splitLine } })
        })
    }
    if (option.legend) {
        option.legend.textStyle = Object.assign({}, option.legend.textStyle, { color: t.textColor })
    }
    return option
}

/**
 * 监听暗黑模式切换（html.class变化），返回取消函数
 */
export function onThemeChange(callback) {
    const observer = new MutationObserver(() => callback(isDark()))
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
    return () => observer.disconnect()
}
