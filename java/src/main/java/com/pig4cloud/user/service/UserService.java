package com.pig4cloud.user.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.user.dto.PasswordUpdateDto;
import com.pig4cloud.user.dto.ResetPasswordDto;
import com.pig4cloud.user.dto.UserCreateDto;
import com.pig4cloud.user.dto.UserDeleteDto;
import com.pig4cloud.user.dto.UserDto;
import com.pig4cloud.user.dto.UserUpdateDto;
import com.pig4cloud.user.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface UserService {

    /**
     * 根据用户名获取用户信息
     */
    R<UserVO> getUser(String username);

    /**
     * 创建用户
     */
    R<Void> createUser(UserCreateDto dto);

    /**
     * 删除用户
     */
    R<Void> deleteUser(UserDeleteDto dto);

    /**
     * 分页获取用户列表
     */
    R<PageResult<Map<String, Object>>> getUserLists(UserDto userDto);

    /**
     * 修改密码
     */
    R<Void> updatePassword(PasswordUpdateDto dto);

    /**
     * 重置密码
     */
    R<Void> resetPassword(ResetPasswordDto dto);

    /**
     * 更新用户信息及角色关联
     */
    R<Void> updateUser(UserUpdateDto dto);

    /**
     * 更新用户头像
     */
    R<Void> updateAvatar(Long userId, MultipartFile avatar) throws IOException;
}
