package com.pig4cloud.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_tenant")
public class TenantEntity {

    /**
     * 租户id（0为平台层）
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 租户编码
     */
    private String tenant_code;

    /**
     * 租户名称
     */
    private String tenant_name;

    /**
     * 状态(0禁用1启用)
     */
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;
}
