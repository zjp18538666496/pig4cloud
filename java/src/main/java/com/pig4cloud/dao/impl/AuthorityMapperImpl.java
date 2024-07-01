package com.pig4cloud.dao.impl;

import com.pig4cloud.dao.AuthorityMapper;
import com.pig4cloud.entity.AuthorityEntity;

import java.util.List;

//        @Select("""
//            SELECT p.*
//            FROM sys_user u
//            INNER JOIN user_role u_r ON u.id = u_r.user_id
//            INNER JOIN sys_role r ON r.id = u_r.role_id
//            INNER JOIN role_permission r_p ON r.id = r_p.role_id
//            INNER JOIN sys_permission p ON p.id = r_p.permission_id
//            WHERE u.username = #{username}""")

public abstract class AuthorityMapperImpl implements AuthorityMapper {
    @Override
    public List<AuthorityEntity> selectAuthorityByUsername(String username) {
        return List.of(
                new AuthorityEntity(1, "login", "登录权限"),
                new AuthorityEntity(2, "root", "管理员")
        );
//        List<AuthorityEntity> list = null;
//        list.addAll(Arrays.asList(new AuthorityEntity(1, "login", "登录权限"), new AuthorityEntity(2, "root", "管理员")));
//        return list;
    }
}
