package com.pig4cloud.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("sys_menu")
@Data
public class MenuEntity {
    // id
    private Integer id;

    // 父级id
    private Integer parentId;

    // 菜单名称
    private String menuName;

    // 菜单路由地址
    private String route;

    // 菜单状态
    private String status;

    // 菜单类型 (0: 目录, 1: 菜单, 2: 按钮)
    private String type;

    // 级别
    private String level;
}
