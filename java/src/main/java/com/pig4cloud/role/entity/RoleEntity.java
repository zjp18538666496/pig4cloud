package com.pig4cloud.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

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
     * 自定义部门集（data_scope=4时生效，逗号分隔部门id）
     */
    private String custom_dept_ids;

    /**
     * 父角色id(0为顶级)；菜单/按钮权限沿父链继承，角色编码与数据权限不继承
     */
    private Integer parent_id;

    /**
     * 软删除标记（1=在回收站；@TableLogic使MP查询/删除自动过滤）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 删除时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date delete_time;
}
