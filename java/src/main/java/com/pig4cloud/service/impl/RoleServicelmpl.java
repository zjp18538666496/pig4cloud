package com.pig4cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.RoleMapper;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.dto.RoleDto;
import com.pig4cloud.entity.RoleEntity;
import com.pig4cloud.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
public class RoleServicelmpl implements RoleService {
    @Autowired
    private RoleMapper roleMapper;

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
    public Response createRole(RoleEntity roleEntity) {
        int rows = roleMapper.insert(roleEntity);
        return new ResponseImpl(200, rows > 0 ? "创建成功" : "创建失败", null);
    }

    /**
     * 修改角色信息
     */
    @Override
    public Response updateRole(RoleEntity roleEntity) {
        UpdateWrapper<RoleEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", roleEntity.getId());
        updateWrapper.set("role_code", roleEntity.getRoleCode());
        updateWrapper.set("role_name", roleEntity.getRoleName());
        updateWrapper.set("description", roleEntity.getDescription());
        int rows = roleMapper.update(roleEntity, updateWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "更新成功" : "更新失败", null);
    }

    /**
     * 删除角色
     */
    @Override
    public Response deleteRole(RoleEntity roleEntity) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", roleEntity.getRoleCode());
        int rows = roleMapper.delete(queryWrapper);
        return new ResponseImpl(rows > 0 ? 200 : -200, rows > 0 ? "删除成功" : "删除失败", null);
    }

    /**
     * 获取角色列表
     */
    @Override
    public Response getRoleLists(RoleDto roleDto) {
        long page = roleDto.getPage();
        long pageSize = roleDto.getPageSize();
        String roleName = roleDto.getRoleName();
        Page<RoleEntity> rowPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<RoleEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (!roleName.isEmpty()) {
            queryWrapper.like(RoleEntity::getRoleName, roleName);
        }
        rowPage = roleMapper.selectPage(rowPage, queryWrapper);
        Response response = new ResponseImpl(200, "获取数据成功", null);
        response.pagination(rowPage);
        return response;
    }
}
