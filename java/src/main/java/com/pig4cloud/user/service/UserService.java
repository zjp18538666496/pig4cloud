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
import java.util.List;
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
     * 导出用全量行（按当前筛选+数据权限，上限1万行）
     */
    List<Map<String, Object>> exportRows(UserDto userDto);

    /**
     * 批量导入用户（逐行按密码策略校验），返回{成功数, 失败明细}
     */
    Map<String, Object> importUsers(List<UserCreateDto> rows);

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
    R<Void> updateAvatar(Long userId, MultipartFile avatar) throws Exception;
}
