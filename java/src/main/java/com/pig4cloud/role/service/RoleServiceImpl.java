package com.pig4cloud.role.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.menu.mapper.MenuMapper;
import com.pig4cloud.role.dto.RoleCreateDto;
import com.pig4cloud.role.dto.RoleDeleteDto;
import com.pig4cloud.role.dto.RoleDto;
import com.pig4cloud.role.dto.RoleUpdateDto;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    @Override
    @Transactional
    public R<Void> createRole(RoleCreateDto dto) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRole_name(dto.getRoleName());
        roleEntity.setRole_code(dto.getRoleCode());
        roleEntity.setDescription(dto.getDescription());
        int rows = roleMapper.insert(roleEntity);
        if (rows <= 0) {
            return R.fail("创建失败");
        }
        saveRoleMenus(roleEntity.getId(), dto.getMenuCodes());
        return R.ok("更新成功", null);
    }

    @Override
    @Transactional
    public R<Void> updateRole(RoleUpdateDto dto) {
        UpdateWrapper<RoleEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", dto.getId());
        updateWrapper.set("role_code", dto.getRoleCode());
        updateWrapper.set("role_name", dto.getRoleName());
        updateWrapper.set("description", dto.getDescription());
        int rows = roleMapper.update(null, updateWrapper);
        if (rows <= 0) {
            return R.fail("更新失败");
        }
        menuMapper.deleteMenus(dto.getId());
        saveRoleMenus(dto.getId(), dto.getMenuCodes());
        return R.ok("更新成功", null);
    }

    @Override
    public R<Void> deleteRole(RoleDeleteDto dto) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", dto.getRoleCode());
        int rows = roleMapper.delete(queryWrapper);
        return R.ok(rows > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R<?> getRoleLists(RoleDto roleDto) {
        long page = roleDto.getPage();
        long pageSize = roleDto.getPageSize();
        if (page >= 0 && pageSize >= 0) {
            String roleName = roleDto.getRoleName();
            List<Map<String, Object>> list = roleMapper.selectList1(roleName, pageSize, page - 1);
            long total = roleMapper.selectUserList2Count();
            return R.ok("获取数据成功", PageResult.of(list, total, pageSize, page));
        }
        List<RoleEntity> rows = roleMapper.selectList(new QueryWrapper<>());
        return R.ok("获取数据成功", rows);
    }

    /**
     * 重建角色的菜单关联
     */
    private void saveRoleMenus(Integer roleId, List<String> menuCodes) {
        List<String> codes = menuCodes == null ? List.of() : menuCodes;
        if (codes.isEmpty()) {
            return;
        }
        List<Map<String, Object>> roleMenus = codes.stream()
                .map(menuId -> {
                    Map<String, Object> roleMenu = new HashMap<>();
                    roleMenu.put("menu_id", Integer.parseInt(menuId.trim()));
                    roleMenu.put("role_id", roleId);
                    return roleMenu;
                })
                .toList();
        int insertResult = menuMapper.insertUserRoles(roleMenus);
        if (insertResult <= 0) {
            throw new BizException("更新角色菜单关联失败");
        }
    }
}
