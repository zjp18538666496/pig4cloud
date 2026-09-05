package com.pig4cloud.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 通知渠道（平台级，sys_notify_channel为拦截器忽略表）：
 * email(邮件)/webhook(通用Webhook)/dingtalk(钉钉)/wecom(企业微信)/feishu(飞书)
 */
@Data
@TableName("sys_notify_channel")
public class SysNotifyChannelEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 渠道名称
     */
    private String channel_name;

    /**
     * 类型(email/webhook/dingtalk/wecom/feishu)
     */
    private String channel_type;

    /**
     * 渠道配置(JSON)：
     * email: {"to":"a@x.com,b@y.com"}
     * webhook/dingtalk/wecom/feishu: {"url":"...","secret":"加签密钥(仅钉钉)"}
     */
    private String config;

    /**
     * 状态(0停用1启用)
     */
    private String status;

    private String remark;

    private String create_by;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
