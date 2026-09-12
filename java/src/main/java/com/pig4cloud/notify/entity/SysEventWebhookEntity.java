package com.pig4cloud.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 事件出站Webhook（平台级，sys_event_webhook为拦截器忽略表）：
 * 订阅内部事件(逗号分隔编码)，事件发生时以HMAC-SHA256签名POST到url
 */
@Data
@TableName("sys_event_webhook")
public class SysEventWebhookEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String webhook_name;

    /**
     * 接收地址
     */
    private String url;

    /**
     * 签名密钥(HMAC-SHA256，可空)
     */
    private String secret;

    /**
     * 订阅事件(逗号分隔，如notice-publish,approval-result)
     */
    private String events;

    /**
     * 状态(0停用1启用)
     */
    private String status;

    private String create_by;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
