package com.pig4cloud.post.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 岗位（租户隔离，sys_post带tenant_id列走拦截器）
 */
@Data
@TableName("sys_post")
public class SysPostEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 岗位编码
     */
    private String post_code;

    /**
     * 岗位名称
     */
    private String post_name;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 状态(0停用1启用)
     */
    private String status;

    /**
     * 租户id
     */
    private Integer tenant_id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
