package com.pig4cloud.menu.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@TableName("sys_menu")
public class MenuEntity {

    /**
     * id（编码规则：父id*100+序号）
     */
    private Integer id;

    /**
     * 父级id，顶级为0
     */
    private Integer parent_id;

    /**
     * 菜单名称
     */
    private String menu_name;

    /**
     * 菜单路由地址
     */
    private String route;

    /**
     * 菜单状态
     */
    private String status;

    /**
     * 菜单类型 (0: 目录, 1: 菜单, 2: 按钮)
     */
    private String type;

    /**
     * 路由组件地址
     */
    private String component_path;

    /**
     * 路由组件名称
     */
    private String component_name;

    /**
     * 层级
     */
    private String level;

    @TableField(exist = false)
    private Boolean disabled = false;

    @TableField(exist = false)
    private List<MenuEntity> children = new ArrayList<>();

    public Boolean getDisabled() {
        return !Objects.equals(this.type, "0");
    }
}
