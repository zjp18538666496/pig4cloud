package com.pig4cloud.service.impl;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.RoleMapper;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.entity.RoleEntity;
import com.pig4cloud.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
public class RoleServicelmpl implements RoleService {
    @Autowired
    private RoleMapper RoleMapper;

    /**
     * 根据角色名获取角色信息
     *
     */
    @Override
    public Response getRole(String rename) {
        return new ResponseImpl(200, "test", null);
    }

    /**
     * 创建角色
     *
     */
    @Override
    public Response createRole(RoleEntity RoleEntity) {
        return new ResponseImpl(200, "test", null);
    }

    /**
     * 修改角色信息
     *
     */
    @Override
    public Response updateRole(RoleEntity RoleEntity) {
        return new ResponseImpl(200, "test", null);
    }

    /**
     * 删除角色
     *
     */
    @Override
    public Response deleteRole(RoleEntity RoleEntity) {
        return new ResponseImpl(200, "test", null);
    }

    /**
     * 获取角色列表
     */
    @Override
    public Response getRoleLists() {
        return new ResponseImpl(200, "test", null);
    }
}
