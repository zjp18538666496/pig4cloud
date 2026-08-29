package com.pig4cloud.menu.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 菜单查询参数：flatMenu返回平铺列表，否则返回树结构
 */
@Getter
@Setter
public class MenuSelectDto {

    private String menuType;
}
