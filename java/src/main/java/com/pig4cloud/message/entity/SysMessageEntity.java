package com.pig4cloud.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 站内信：发送时按目标用户扇出落库（sys_message带tenant_id列走租户拦截器）
 */
@Data
@TableName("sys_message")
public class SysMessageEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 类型(1系统消息2公告通知)
     */
    private String msg_type;

    /**
     * 租户id(0为平台)
     */
    private Integer tenant_id;

    /**
     * 目标用户id
     */
    private Integer target_user_id;

    /**
     * 关联公告id（公告扇出站内信时记录，已读回执统计用）
     */
    private Integer notice_id;

    /**
     * 已读(0未读1已读)
     */
    private String read_flag;

    /**
     * 发送人
     */
    private String create_by;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;
}
