package com.pig4cloud.menu.service;

import com.pig4cloud.menu.entity.MenuEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单平铺列表转树结构
 */
public final class MenuTreeBuilder {

    private MenuTreeBuilder() {
    }

    /**
     * 按parent_id组装树，parent_id为0的为根节点；
     * 父节点不在列表中的孤儿节点会被丢弃，单个元素列表原样返回（保持历史行为）
     */
    public static List<MenuEntity> build(List<MenuEntity> menus) {
        List<MenuEntity> rootMenus = new ArrayList<>();
        if (menus == null || menus.isEmpty()) {
            return rootMenus;
        }
        if (menus.size() == 1) {
            return menus;
        }

        Map<Integer, MenuEntity> menuMap = new HashMap<>();
        for (MenuEntity menu : menus) {
            menuMap.put(menu.getId(), menu);
        }

        for (MenuEntity menu : menus) {
            if (menu.getParent_id() == 0) {
                rootMenus.add(menu);
            } else {
                MenuEntity parentMenu = menuMap.get(menu.getParent_id());
                if (parentMenu != null) {
                    parentMenu.getChildren().add(menu);
                }
            }
        }
        return rootMenus;
    }
}
