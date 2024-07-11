package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.dao.MenuMapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.dto.MenuDto;
import com.pig4cloud.entity.MenuEntity;
import com.pig4cloud.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Component
public class MenuServiceImpl implements MenuService {
    @Autowired
    private MenuMapper menuMapper;

    @Override
    public Response createMenu(MenuEntity menuEntity) {
        int rows = menuMapper.insert(menuEntity);
        return new ResponseImpl(200, rows > 0 ? "创建成功" : "创建失败", null);
    }

    @Override
    public Response updateMenu(MenuEntity menuEntity) {
        Integer parent_id = menuEntity.getParent_id();
        if (parent_id == null) {
            return new ResponseImpl(-200, "父级菜单不能为空！", null);
        }
        if (parent_id == 0) {

        } else {
            QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", parent_id);
            List<MenuEntity> parentMenu = menuMapper.selectList(queryWrapper);
        }


        UpdateWrapper<MenuEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", menuEntity.getId());
        int rows = menuMapper.update(menuEntity, updateWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "更新成功" : "更新失败", null);
    }

    @Override
    public Response deleteMenu(MenuEntity menuEntity) {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", menuEntity.getId());
        int rows = menuMapper.delete(queryWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    public Response getMenuLists(MenuDto menuDto) {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        String menuName = menuDto.getMenu_name();
        if (menuName != null && !menuName.isEmpty()) {
            queryWrapper.like("menu_name", menuName);
        }
        List<MenuEntity> selectList = menuMapper.selectList(queryWrapper);

        // 构建菜单树
        List<MenuEntity> menus = buildMenuTree(selectList);

        return new ResponseImpl(200, "获取数据成功", menus);
    }

    /**
     * 构建树形菜单
     */
    public List<MenuEntity> buildMenuTree(List<MenuEntity> menus) {
        Map<Integer, MenuEntity> menuMap = new HashMap<>();
        List<MenuEntity> rootMenus = new ArrayList<>();
        if (menus.size() == 1) {
            return menus;
        }

        // 将菜单项放入Map中
        for (MenuEntity menu : menus) {
            menuMap.put(menu.getId(), menu);
        }

        // 构建树结构
        for (MenuEntity menu : menus) {
            if (menu.getParent_id() == 0) {
                rootMenus.add(menu);
            } else {
                MenuEntity parentMenu = menuMap.get(menu.getParent_id());
                if (parentMenu != null) {
                    if (parentMenu.getChildren() == null) {
                        parentMenu.setChildren(new ArrayList<>());
                    }
                    parentMenu.getChildren().add(menu);
                }
            }
        }

        return rootMenus;
    }
}
