package com.pig4cloud.service;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dto.MenuDto;
import com.pig4cloud.entity.MenuEntity;

public interface MenuService {
    /**
     * 创建菜单
     */
    Response createMenu(MenuEntity menuEntity);

    /**
     * 修改菜单信息
     */
    Response updateMenu(MenuEntity menuEntity);

    /**
     * 删除菜单
     */
    Response deleteMenu(MenuEntity menuEntity);

    /**
     * 获取菜单列表
     */
    Response getMenuLists(MenuDto menuDto);


    Response selectMenuLists(MenuDto menuDto);
}
