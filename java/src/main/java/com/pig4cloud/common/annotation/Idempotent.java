package com.pig4cloud.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交：标注在写接口上，同一用户+同一URI+相同参数在intervalSeconds内
 * 只放行一次，重复请求返回友好提示（基于StateStore，多实例共享）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等窗口（秒）
     */
    int intervalSeconds() default 3;
}
