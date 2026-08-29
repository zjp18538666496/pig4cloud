package com.pig4cloud.role.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.role.dto.RoleCreateDto;
import com.pig4cloud.role.dto.RoleDeleteDto;
import com.pig4cloud.role.dto.RoleDto;
import com.pig4cloud.role.dto.RoleUpdateDto;
import com.pig4cloud.role.entity.RoleEntity;

import java.util.Map;

public interface RoleService {

    /**
     * 创建角色并关联菜单
     */
    R<Void> createRole(RoleCreateDto dto);

    /**
     * 修改角色信息及菜单关联
     */
    R<Void> updateRole(RoleUpdateDto dto);

    /**
     * 删除角色
     */
    R<Void> deleteRole(RoleDeleteDto dto);

    /**
     * 获取角色列表
     */
    R<?> getRoleLists(RoleDto roleDto);
}
