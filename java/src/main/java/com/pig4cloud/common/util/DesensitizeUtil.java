package com.pig4cloud.common.util;

/**
 * 敏感字段脱敏：手机号保留前3后4，邮箱保留首字符与域名
 */
public class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@") || email.indexOf("@") == 0) {
            return email;
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "****" + email.substring(at);
    }
}
