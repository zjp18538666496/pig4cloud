package com.pig4cloud.common.i18n;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 后端消息国际化（轻量实现）：按Accept-Language返回中/英文消息，默认中文。
 * 覆盖核心链路（认证/二次认证/通用异常），业务消息默认中文；新增词条在
 * resources/i18n/messages*.properties中维护。前端已有中英文切换，请求头Accept-Language随之变化
 */
public final class I18nMessage {

    private static final Map<String, String> ZH = Map.ofEntries(
            Map.entry("login.wrong-credentials", "用户名或密码不正确"),
            Map.entry("login.locked", "登录失败次数过多，请稍后再试"),
            Map.entry("login.captcha-required", "验证码不能为空"),
            Map.entry("login.captcha-wrong", "验证码错误"),
            Map.entry("login.sms-expired", "短信验证码已过期，请重新获取"),
            Map.entry("login.sms-wrong", "短信验证码错误"),
            Map.entry("login.account-missing", "账号不存在"),
            Map.entry("reauth.failed", "二次认证失败：请携带当前登录账号密码（请求头X-Reauth-Password）"),
            Map.entry("common.error", "系统异常，请稍后重试"));

    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("login.wrong-credentials", "Incorrect username or password"),
            Map.entry("login.locked", "Too many failed attempts, please retry later"),
            Map.entry("login.captcha-required", "Captcha is required"),
            Map.entry("login.captcha-wrong", "Incorrect captcha"),
            Map.entry("login.sms-expired", "SMS code expired, please request a new one"),
            Map.entry("login.sms-wrong", "Incorrect SMS code"),
            Map.entry("login.account-missing", "Account not found"),
            Map.entry("reauth.failed", "Re-authentication failed: current password required (header X-Reauth-Password)"),
            Map.entry("common.error", "System error, please try again later"));

    private I18nMessage() {
    }

    /**
     * 按当前请求语言取消息，无词条时回退中文
     */
    public static String get(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> bundle = locale != null && locale.getLanguage().startsWith("en") ? EN : ZH;
        return bundle.getOrDefault(key, ZH.getOrDefault(key, key));
    }
}
