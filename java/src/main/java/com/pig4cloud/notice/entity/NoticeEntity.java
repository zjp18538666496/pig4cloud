package com.pig4cloud.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 通知公告（sys_notice为拦截器忽略表）：tenant_id=0为平台公告（全员可见），N为指定租户公告
 */
@Data
@TableName("sys_notice")
public class NoticeEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 状态(0草稿1发布)
     */
    private String status;

    /**
     * 租户id(0为平台全员可见)
     */
    private Integer tenant_id;

    /**
     * 发布人
     */
    private String create_by;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
