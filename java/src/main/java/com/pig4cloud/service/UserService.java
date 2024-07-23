package com.pig4cloud.service;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dto.UserDto;
import com.pig4cloud.entity.UserEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

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
    Response getUserLists(UserDto userDto);

    /**
     * 更新密码
     */
    Response updatePassword(UserEntity userEntity);

    /**
     * 忘记密码 重置密码
     */
    Response resetPassword(UserEntity userEntity);

    /**
     * 更新用户信息
     * @param map
     * @return
     */
    Response updateUser1(Map<String, Object> map);

    /**
     * 更新用户头像
     * @param map
     * @return
     * @throws IOException
     */
    Response updateAvatar(Map<String, Object> map) throws IOException;
}
