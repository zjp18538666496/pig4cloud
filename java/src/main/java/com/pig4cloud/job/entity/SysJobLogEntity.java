package com.pig4cloud.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 任务执行日志
 */
@Data
@TableName("sys_job_log")
public class SysJobLogEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务id
     */
    private Integer job_id;

    /**
     * 任务名称
     */
    private String job_name;

    /**
     * 结果(0失败1成功)
     */
    private String success;

    /**
     * 执行信息
     */
    private String message;

    /**
     * 耗时(ms)
     */
    private Long cost_ms;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;
}
