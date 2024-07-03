package com.pig4cloud.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class RoleEntity {

    /**
     * id
     */
    private int id;
    /**
     * 角色名称
     */
    private String roleName;
    /**
     * role_code
     */
    private String roleCode;
    /**
     * 角色描述
     */
    private String description;
}