package com.pig4cloud.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 操作日志文档，存储在MongoDB的operate_log集合
 */
@Getter
@Setter
@Document("operate_log")
@CompoundIndex(def = "{'createTime': -1}")
public class OperateLog {

    @Id
    private String id;

    /**
     * 租户id（登录/注册等未认证操作为null，仅超管可见）
     */
    private Integer tenantId;

    /**
     * 操作人（未认证接口如登录/注册取请求参数中的username）
     */
    private String username;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * 请求方式，如POST
     */
    private String method;

    /**
     * 请求地址
     */
    private String url;

    /**
     * 请求参数（敏感字段脱敏，超长截断）
     */
    private String params;

    /**
     * 来源IP
     */
    private String ip;

    /**
     * 响应业务码
     */
    private Integer code;

    /**
     * 是否成功（code=200）
     */
    private Boolean success;

    /**
     * 异常/失败信息
     */
    private String errorMsg;

    /**
     * 耗时毫秒
     */
    private Long costMs;

    /**
     * 记录时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
