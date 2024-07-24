package com.pig4cloud.dao.impl;

import com.pig4cloud.dao.AuthorityMapper;
import com.pig4cloud.entity.AuthorityEntity;

import java.util.List;

public abstract class AuthorityMapperImpl implements AuthorityMapper {
    @Override
    public List<AuthorityEntity> selectAuthorityByUsername(String username) {
        return List.of(
                new AuthorityEntity(1, "login", "登录权限"),
                new AuthorityEntity(2, "root", "管理员")
        );
    }
}
