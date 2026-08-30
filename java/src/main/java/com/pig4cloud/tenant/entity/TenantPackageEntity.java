package com.pig4cloud.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 租户套餐：平台级配置（sys_tenant_package为拦截器忽略表，不参与租户过滤），
 * menu_ids决定开通租户时租户管理员角色可用的菜单
 */
@Data
@TableName("sys_tenant_package")
public class TenantPackageEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 套餐名称
     */
    private String package_name;

    /**
     * 关联菜单id（逗号分隔）
     */
    private String menu_ids;

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
