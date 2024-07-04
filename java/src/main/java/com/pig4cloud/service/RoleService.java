package com.pig4cloud.service;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dto.RoleDto;
import com.pig4cloud.entity.RoleEntity;

public interface RoleService {

    /**
     * 根据角色名获取角色信息
     */
    Response getRole(String rename);

    /**
     * 创建角色
     */
    Response createRole(RoleEntity RoleEntity);

    /**
     * 修改角色信息
     */
    Response updateRole(RoleEntity RoleEntity);

    /**
     * 删除角色
     */
    Response deleteRole(RoleEntity RoleEntity);

    /**
     * 获取角色列表
     */
    Response getRoleLists(RoleDto roleDto);
}
