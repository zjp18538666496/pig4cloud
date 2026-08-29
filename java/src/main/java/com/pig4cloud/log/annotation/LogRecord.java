package com.pig4cloud.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志埋点：标注在需要记录操作日志的接口方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRecord {

    /**
     * 操作模块，如"用户管理"
     */
    String module();

    /**
     * 操作描述，如"删除用户"
     */
    String operation();
}
