package com.pig4cloud.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 通知消息模板（平台级）：标题/内容支持${变量}占位，发送时按params渲染
 */
@Data
@TableName("sys_notify_template")
public class SysNotifyTemplateEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 模板编码（事件接入点使用，如notice-publish/job-failed）
     */
    private String template_code;

    private String template_name;

    /**
     * 标题模板(支持${变量})
     */
    private String title_template;

    /**
     * 内容模板(支持${变量})
     */
    private String content_template;

    /**
     * 状态(0停用1启用)
     */
    private String status;

    private String remark;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
