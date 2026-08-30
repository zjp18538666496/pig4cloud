package com.pig4cloud.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 字典（平台级，sys_dict为拦截器忽略表），业务下拉枚举可配置化
 */
@Data
@TableName("sys_dict")
public class SysDictEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 字典编码（唯一，业务按code取项）
     */
    private String dict_code;

    /**
     * 字典名称
     */
    private String dict_name;

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
