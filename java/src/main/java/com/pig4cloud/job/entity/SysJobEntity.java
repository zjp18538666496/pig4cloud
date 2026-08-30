package com.pig4cloud.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 定时任务（平台级，sys_job为拦截器忽略表）
 */
@Data
@TableName("sys_job")
public class SysJobEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务名称
     */
    private String job_name;

    /**
     * 处理器Bean名（对应JobHandler.name()）
     */
    private String handler;

    /**
     * cron表达式
     */
    private String cron;

    /**
     * 状态(0停用1启用)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
