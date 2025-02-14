package com.pig4cloud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@TableName("sys_menu")
@Data
public class MenuEntity {
    // id
    private Integer id;

    // 父级id
    private Integer parent_id;

    // 菜单名称
    private String menu_name;

    // 菜单路由地址
    private String route;

    // 菜单状态
    private String status;

    // 菜单类型 (0: 目录, 1: 菜单, 2: 按钮)
    private String type;

    // 路由组件地址
    private String component_path;

    // 路由组件名称
    private String component_name;

    // 级别
    private String level;

    @TableField(exist = false)
    private Boolean disabled = false;

    @TableField(exist = false)
    private List<MenuEntity> children = new ArrayList<>();

    public Boolean getDisabled() {
        return !Objects.equals(this.type, "0");
    }
}
