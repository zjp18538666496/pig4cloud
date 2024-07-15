package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.dao.MenuMapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.dto.MenuDto;
import com.pig4cloud.entity.MenuEntity;
import com.pig4cloud.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Component
public class MenuServiceImpl implements MenuService {
    @Autowired
    private MenuMapper menuMapper;

    @Override
    public Response createMenu(MenuEntity menuEntity) {
        Integer newId = 0;
        // 获取新的父菜单下的最大ID
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", menuEntity.getParent_id());
        newId = menuMapper.getMaxAgeByCondition(queryWrapper);
        if (newId == null) {
            newId = menuEntity.getParent_id() * 100 + 1;
        } else {
            newId += 1;
        }
        menuEntity.setId(newId);
        int rows = menuMapper.insert(menuEntity);
        return new ResponseImpl(200, rows > 0 ? "创建成功" : "创建失败", null);
    }

    @Override
    @Transactional
    public Response updateMenu(MenuEntity menuEntity) {
        Integer parentId = menuEntity.getParent_id();

        if (parentId == null) {
            return new ResponseImpl(-200, "父级菜单不能为空！", null);
        }

        // 查询当前菜单信息
        MenuEntity currentMenu = menuMapper.selectById(menuEntity.getId());
        if (currentMenu == null) {
            return new ResponseImpl(-200, "未找到当前菜单！", null);
        }

        Integer newId = 0;

        // 检查是否修改了上级菜单，若修改则判断是否有子菜单
        if (!currentMenu.getParent_id().equals(parentId)) {
            List<MenuEntity> submenuList = getSubmenuList(menuEntity.getId());
            if (!submenuList.isEmpty()) {
                return new ResponseImpl(-200, "当前菜单存在子级菜单，请删除子级菜单后再修改该菜单的上级菜单！", null);
            }

            // 获取新的父菜单下的最大ID
            QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", parentId);
            newId = menuMapper.getMaxAgeByCondition(queryWrapper);
            if (newId == null) {
                newId = menuEntity.getParent_id() * 100 + 1;
            } else {
                newId += 1;
            }
        }

        // 检查是否修改了菜单类型，若修改则判断是否有子菜单
        if (!currentMenu.getType().equals(menuEntity.getType())) {
            List<MenuEntity> submenuList = getSubmenuList(menuEntity.getId());
            if (!submenuList.isEmpty()) {
                return new ResponseImpl(-200, "当前菜单存在子级菜单，请删除子级菜单后再修改该菜单的类型！", null);
            }
        }

        if (newId > 0) {
            // 删除旧菜单
            int deleteRows = menuMapper.deleteById(menuEntity.getId());
            if (deleteRows <= 0) {
                return new ResponseImpl(-200, "更新失败", null);
            }

            // 插入新菜单
            menuEntity.setId(newId);
            int insertRows = menuMapper.insert(menuEntity);
            return new ResponseImpl(insertRows > 0 ? 200 : -200, insertRows > 0 ? "更新成功" : "更新失败", null);
        } else {
            // 更新菜单信息
            int updateRows = menuMapper.updateById(menuEntity);
            return new ResponseImpl(updateRows > 0 ? 200 : -200, updateRows > 0 ? "更新成功" : "更新失败", null);
        }
    }


    @Override
    @Transactional
    public Response deleteMenu(MenuEntity menuEntity) {
        int deleteCount = deleteMenuAndSubmenus(menuEntity.getId());
        return new ResponseImpl(deleteCount > 0 ? 200 : -200, deleteCount > 0 ? "删除成功" : "删除失败", null);
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

    @Override
    public Response selectMenuLists(Map<String, Object> map) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();
        if (name == null) {
            return new ResponseImpl(-200, "获取用户信息失败", null);
        }
        List<MenuEntity> selectList = menuMapper.selectMenuLists(name);
        String menuType = (String) map.get("menuType");
        if (menuType.equals("flatMenu")) {
            return new ResponseImpl(200, "获取数据成功", selectList);
        }
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

    /**
     * 递归删除菜单及其所有子菜单
     *
     * @param menuId 菜单ID
     * @return 删除的行数
     */
    public int deleteMenuAndSubmenus(Integer menuId) {
        // 获取所有子菜单
        List<MenuEntity> submenus = getSubmenuList(menuId);
        int deleteCount = 0;
        for (MenuEntity submenu : submenus) {
            // 递归删除子菜单
            deleteCount += deleteMenuAndSubmenus(submenu.getId());
        }
        // 删除当前菜单
        deleteCount += menuMapper.deleteById(menuId);
        return deleteCount;
    }

    private List<MenuEntity> getSubmenuList(Integer parentId) {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        return menuMapper.selectList(queryWrapper);
    }
}
