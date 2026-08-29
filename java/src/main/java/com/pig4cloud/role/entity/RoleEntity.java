package com.pig4cloud.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class RoleEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 角色编码
     */
    private String role_code;

    /**
     * 角色名称
     */
    private String role_name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 租户id（0为平台层角色）
     */
    private Integer tenant_id;
}
