package com.pig4cloud.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class RoleEntity {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
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

    /**
     * 数据权限(1本租户全部2本部门及以下3仅本人)；不随父链继承
     */
    private String data_scope;

    /**
     * 父角色id(0为顶级)；菜单/按钮权限沿父链继承，角色编码与数据权限不继承
     */
    private Integer parent_id;
}
