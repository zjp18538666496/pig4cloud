package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.dao.MenuMapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.RoleMapper;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.dto.RoleDto;
import com.pig4cloud.entity.RoleEntity;
import com.pig4cloud.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Component
public class RoleServicelmpl implements RoleService {
    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MenuMapper menuMapper;

    /**
     * 根据角色名获取角色信息
     */
    @Override
    public Response getRole(String rename) {
        return null;
    }

    /**
     * 创建角色
     */
    @Override
    @Transactional
    public Response createRole(Map<String, Object> map) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRole_name((String) map.get("role_name"));
        roleEntity.setRole_code((String) map.get("role_code"));
        roleEntity.setDescription((String) map.get("description"));
        int rows = roleMapper.insert(roleEntity);
        if (rows > 0) {
            int id = roleEntity.getId();
            String menuCodes = map.get("menu_codes").toString().trim();
            List<String> roleCodes = menuCodes.isEmpty() ? Collections.emptyList() : List.of(menuCodes.split(","));

            List<Map<String, Object>> roles = roleCodes.stream()
                    .map(menuId -> {
                        Map<String, Object> role = new HashMap<>();
                        Integer menuIdAsInteger = Integer.parseInt(menuId);
                        role.put("menu_id", menuIdAsInteger);
                        role.put("role_id", id);
                        return role;
                    })
                    .collect(Collectors.toList());
            // 插入新的角色关联
            if (!roleCodes.isEmpty()) {
                int insertResult = menuMapper.insertUserRoles(roles);
                if (insertResult > 0) {
                    return new ResponseImpl(200, "更新成功", null);
                } else {
                    throw new RuntimeException("更新角色菜单关联失败");
                }
            } else {
                // 如果角色列表为空，直接返回更新成功
                return new ResponseImpl(200, "更新成功", null);
            }
        }
        return new ResponseImpl(-200, "更新失败", null);
    }

    /**
     * 修改角色信息
     */
    @Override
    @Transactional
    public Response updateRole(Map<String, Object> map) {
        UpdateWrapper<RoleEntity> updateWrapper = new UpdateWrapper<>();
        Integer id = (Integer) map.get("id");
        updateWrapper.eq("id", id);
        updateWrapper.set("role_code", map.get("role_code"));
        updateWrapper.set("role_name", map.get("role_name"));
        updateWrapper.set("description", map.get("description"));
        int rows = roleMapper.update(null, updateWrapper);
        if (rows > 0) {
            String menuCodes = map.get("menu_codes").toString().trim();
            List<String> roleCodes = menuCodes.isEmpty() ? Collections.emptyList() : List.of(menuCodes.split(","));

            List<Map<String, Object>> roles = roleCodes.stream()
                    .map(menuId -> {
                        Map<String, Object> role = new HashMap<>();
                        Integer menuIdAsInteger = Integer.parseInt(menuId);
                        role.put("menu_id", menuIdAsInteger);
                        role.put("role_id", id);
                        return role;
                    })
                    .collect(Collectors.toList());


            // 删除原有角色关联
            int deleteResult = menuMapper.deleteMenus((Integer) map.get("id"));

            // 插入新的角色关联
            if (!roleCodes.isEmpty()) {
                int insertResult = menuMapper.insertUserRoles(roles);
                if (deleteResult >= 0 && insertResult > 0) {
                    return new ResponseImpl(200, "更新成功", null);
                } else {
                    throw new RuntimeException("更新用户角色关联失败");
                }
            } else {
                // 如果角色列表为空，直接返回更新成功
                return new ResponseImpl(200, "更新成功", null);
            }
        }
        return new ResponseImpl(-200, "更新失败", null);
    }

    /**
     * 删除角色
     */
    @Override
    public Response deleteRole(RoleEntity roleEntity) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", roleEntity.getRole_code());
        int rows = roleMapper.delete(queryWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "删除成功" : "删除失败", null);
    }

    /**
     * 获取角色列表
     */
    public Response getRoleLists(RoleDto roleDto) {
        long page = roleDto.getPage();
        long pageSize = roleDto.getPageSize();
        String roleName = roleDto.getRoleName();
        if (page >= 0 && pageSize >= 0) {
            List<Map<String, Object>> list = roleMapper.selectList1(roleName, pageSize, page - 1);
            long total = roleMapper.selectUserList2Count();
            Response response = new ResponseImpl(200, "获取数据成功", null);
            response.pagination(list, total, pageSize, page);
            return response;
        } else {
            QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
            List<RoleEntity> rows = roleMapper.selectList(queryWrapper);
            return new ResponseImpl(200, "获取数据成功", rows);
        }
    }
}
