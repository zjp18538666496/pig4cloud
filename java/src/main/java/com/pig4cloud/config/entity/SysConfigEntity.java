package com.pig4cloud.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 系统参数配置（平台级，sys_config为拦截器忽略表）：
 * 密码策略/登录锁定阈值/日志保留天数等运行参数，界面可改、即时生效
 */
@Data
@TableName("sys_config")
public class SysConfigEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 配置键（唯一，创建后不可改）
     */
    private String config_key;

    /**
     * 配置名称
     */
    private String config_name;

    /**
     * 配置值
     */
    private String config_value;

    /**
     * 备注/可选值说明
     */
    private String remark;

    /**
     * 最后修改人
     */
    private String update_by;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
