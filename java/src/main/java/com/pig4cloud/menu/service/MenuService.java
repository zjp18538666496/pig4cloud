package com.pig4cloud.menu.service;

import com.pig4cloud.common.result.R;
import com.pig4cloud.menu.dto.MenuDto;
import com.pig4cloud.menu.dto.MenuSelectDto;
import com.pig4cloud.menu.entity.MenuEntity;

import java.util.List;

public interface MenuService {

    /**
     * 创建菜单
     */
    R<Void> createMenu(MenuEntity menuEntity);

    /**
     * 修改菜单信息
     */
    R<Void> updateMenu(MenuEntity menuEntity);

    /**
     * 删除菜单及其子菜单
     */
    R<Void> deleteMenu(MenuEntity menuEntity);

    /**
     * 获取菜单树（管理端）
     */
    R<List<MenuEntity>> getMenuLists(MenuDto menuDto);

    /**
     * 获取当前用户有权限的菜单
     */
    R<?> selectMenuLists(MenuSelectDto dto);
}
