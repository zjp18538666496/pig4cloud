import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN.js'
import en from './en.js'

const LOCALE_KEY = 'locale'

const getLocale = () => localStorage.getItem(LOCALE_KEY) || 'zh-CN'

const i18n = createI18n({
    legacy: false,
    globalInjection: true,
    locale: getLocale(),
    fallbackLocale: 'zh-CN',
    messages: {
        'zh-CN': zhCN,
        en,
    },
})

export const setLocale = (locale) => {
    i18n.global.locale.value = locale
    localStorage.setItem(LOCALE_KEY, locale)
    document.documentElement.setAttribute('lang', locale)
}

export const getLocaleConfig = getLocale

export default i18n
