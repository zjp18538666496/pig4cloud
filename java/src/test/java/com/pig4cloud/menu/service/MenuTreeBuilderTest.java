package com.pig4cloud.menu.service;

import com.pig4cloud.menu.entity.MenuEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuTreeBuilderTest {

    private MenuEntity menu(int id, int parentId, String type) {
        MenuEntity menu = new MenuEntity();
        menu.setId(id);
        menu.setParent_id(parentId);
        menu.setType(type);
        return menu;
    }

    @Test
    void buildsTreeFromFlatList() {
        List<MenuEntity> flat = List.of(
                menu(2, 0, "0"),
                menu(201, 2, "0"),
                menu(20101, 201, "1"));

        List<MenuEntity> tree = MenuTreeBuilder.build(flat);

        assertEquals(1, tree.size());
        assertEquals(2, tree.get(0).getId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(201, tree.get(0).getChildren().get(0).getId());
        assertEquals(1, tree.get(0).getChildren().get(0).getChildren().size());
    }

    @Test
    void orphanNodesAreDropped() {
        List<MenuEntity> flat = List.of(menu(2, 0, "0"), menu(999, 888, "1"));

        List<MenuEntity> tree = MenuTreeBuilder.build(flat);

        assertEquals(1, tree.size());
        assertTrue(tree.get(0).getChildren().isEmpty());
    }

    @Test
    void singleItemReturnsAsIs() {
        // 保持历史行为：单个菜单（可能是孤儿节点）原样返回，避免不可见
        List<MenuEntity> flat = List.of(menu(20101, 201, "1"));

        assertSame(flat, MenuTreeBuilder.build(flat));
    }

    @Test
    void emptyOrNullReturnsEmpty() {
        assertTrue(MenuTreeBuilder.build(List.of()).isEmpty());
        assertTrue(MenuTreeBuilder.build(null).isEmpty());
    }
}
