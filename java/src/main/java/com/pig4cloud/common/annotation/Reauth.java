package com.pig4cloud.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感操作二次认证：标注在Controller方法上，请求须携带请求头X-Reauth-Password
 * （当前登录账号密码），校验通过才放行；reauth.enabled=false时全局关闭。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reauth {
}
