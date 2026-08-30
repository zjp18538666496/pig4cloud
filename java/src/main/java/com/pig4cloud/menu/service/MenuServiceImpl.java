package com.pig4cloud.menu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.R;
import com.pig4cloud.menu.dto.MenuDto;
import com.pig4cloud.menu.dto.MenuSelectDto;
import com.pig4cloud.menu.entity.MenuEntity;
import com.pig4cloud.menu.mapper.MenuMapper;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.role.service.RoleHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final RoleHierarchyService roleHierarchyService;

    @Override
    public R<Void> createMenu(MenuEntity menuEntity) {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", menuEntity.getParent_id());
        Integer newId = menuMapper.getMaxAgeByCondition(queryWrapper);
        newId = newId == null ? menuEntity.getParent_id() * 100 + 1 : newId + 1;
        menuEntity.setLevel(calcMenuLevel(newId));
        menuEntity.setId(newId);
        int rows = menuMapper.insert(menuEntity);
        return R.ok(rows > 0 ? "创建成功" : "创建失败", null);
    }

    @Override
    @Transactional
    public R<Void> updateMenu(MenuEntity menuEntity) {
        Integer parentId = menuEntity.getParent_id();
        if (parentId == null) {
            throw new BizException("父级菜单不能为空！");
        }
        MenuEntity currentMenu = menuMapper.selectById(menuEntity.getId());
        if (currentMenu == null) {
            throw new BizException("未找到当前菜单！");
        }

        Integer newId = 0;

        // 修改了上级菜单时，不允许存在子菜单，并重新计算菜单id
        if (!currentMenu.getParent_id().equals(parentId)) {
            if (!getSubmenuList(menuEntity.getId()).isEmpty()) {
                throw new BizException("当前菜单存在子级菜单，请删除子级菜单后再修改该菜单的上级菜单！");
            }
            QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", parentId);
            newId = menuMapper.getMaxAgeByCondition(queryWrapper);
            newId = newId == null ? menuEntity.getParent_id() * 100 + 1 : newId + 1;
            menuEntity.setLevel(calcMenuLevel(newId));
        }

        // 修改了菜单类型时，不允许存在子菜单
        if (!currentMenu.getType().equals(menuEntity.getType()) && !getSubmenuList(menuEntity.getId()).isEmpty()) {
            throw new BizException("当前菜单存在子级菜单，请删除子级菜单后再修改该菜单的类型！");
        }

        if (newId > 0) {
            // id承载层级语义，换父级时删除旧记录再插入新id
            int deleteRows = menuMapper.deleteById(menuEntity.getId());
            if (deleteRows <= 0) {
                return R.fail("更新失败");
            }
            menuEntity.setId(newId);
            int insertRows = menuMapper.insert(menuEntity);
            return R.ok(insertRows > 0 ? "更新成功" : "更新失败", null);
        }
        int updateRows = menuMapper.updateById(menuEntity);
        return R.ok(updateRows > 0 ? "更新成功" : "更新失败", null);
    }

    @Override
    @Transactional
    public R<Void> deleteMenu(MenuEntity menuEntity) {
        int deleteCount = deleteMenuAndSubmenus(menuEntity.getId());
        return R.ok(deleteCount > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    public R<List<MenuEntity>> getMenuLists(MenuDto menuDto) {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        String menuName = menuDto.getMenu_name();
        if (menuName != null && !menuName.isEmpty()) {
            queryWrapper.like("menu_name", menuName);
        }
        List<MenuEntity> selectList = menuMapper.selectList(queryWrapper);
        return R.ok("获取数据成功", MenuTreeBuilder.build(selectList));
    }

    @Override
    public R<?> selectMenuLists(MenuSelectDto dto) {
        String name = currentUsername();
        // 当前用户的菜单 = 自身角色+祖先角色（沿父链继承）绑定的菜单
        List<RoleEntity> userRoles = roleMapper.selectRolesByUsername(name);
        List<Integer> effectiveRoleIds = roleHierarchyService.effectiveRoleIds(userRoles);
        List<MenuEntity> selectList = effectiveRoleIds.isEmpty()
                ? List.of() : menuMapper.selectMenusByRoleIds(effectiveRoleIds);
        if (dto != null && "flatMenu".equals(dto.getMenuType())) {
            return R.ok("获取数据成功", selectList);
        }
        return R.ok("获取数据成功", MenuTreeBuilder.build(selectList));
    }

    /**
     * 递归删除菜单及其所有子菜单，返回删除的行数
     */
    private int deleteMenuAndSubmenus(Integer menuId) {
        int deleteCount = 0;
        for (MenuEntity submenu : getSubmenuList(menuId)) {
            deleteCount += deleteMenuAndSubmenus(submenu.getId());
        }
        deleteCount += menuMapper.deleteById(menuId);
        return deleteCount;
    }

    private List<MenuEntity> getSubmenuList(Integer parentId) {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        return menuMapper.selectList(queryWrapper);
    }

    /**
     * 由菜单id计算层级：层级=父id链深度，如20101为3级
     */
    private String calcMenuLevel(long id) {
        long level = 1L;
        while (id < 1 || id > 99) {
            id = id / 100;
            level++;
        }
        return Long.toString(level);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BizException("获取用户信息失败");
        }
        return authentication.getName();
    }
}
