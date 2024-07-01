package com.pig4cloud.service;

import com.pig4cloud.dao.Response;
import com.pig4cloud.entity.UserEntity;

public interface UserService {

    /**
     * 根据用户名获取用户信息
     */
    Response getUser(String username);

    /**
     * 创建用户
     */
    Response createUser(UserEntity userEntity);

    /**
     * 修改用户信息
     */
    Response updateUser(UserEntity userEntity);

    /**
     * 删除用户
     */
    Response deleteUser(UserEntity userEntity);

    /**
     * 获取用户列表
     */
    Response getUserLists();
}
