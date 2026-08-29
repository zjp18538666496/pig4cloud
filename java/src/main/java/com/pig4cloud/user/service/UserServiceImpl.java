package com.pig4cloud.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.file.service.FtpService;
import com.pig4cloud.file.util.FileUtils;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.user.dto.PasswordUpdateDto;
import com.pig4cloud.user.dto.ResetPasswordDto;
import com.pig4cloud.user.dto.UserCreateDto;
import com.pig4cloud.user.dto.UserDeleteDto;
import com.pig4cloud.user.dto.UserDto;
import com.pig4cloud.user.dto.UserUpdateDto;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import com.pig4cloud.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final FtpService ftpService;
    private final FileUtils fileUtils;

    @Override
    public R<Void> createUser(UserCreateDto dto) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", dto.getUsername());
        if (userMapper.selectOne(queryWrapper) != null) {
            throw new BizException("用户已存在");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(dto.getUsername());
        userEntity.setPassword(passwordEncoder.encode(dto.getPassword()));
        userEntity.setName(dto.getUsername());
        userEntity.setCreate_time(new Timestamp(System.currentTimeMillis()));
        // 租户归属由服务端决定：未认证上下文(注册)归默认租户1，其余取当前登录用户租户
        userEntity.setTenant_id(UserContext.getTenantId() == null ? 1 : UserContext.getTenantId());
        int rows = userMapper.insert(userEntity);
        return R.ok(rows > 0 ? "注册成功" : "注册失败", null);
    }

    @Override
    public R<Void> deleteUser(UserDeleteDto dto) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", dto.getUsername());
        int rows = userMapper.delete(queryWrapper);
        return R.ok(rows > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    public R<PageResult<Map<String, Object>>> getUserLists(UserDto userDto) {
        long page = userDto.getPage();
        long pageSize = userDto.getPageSize();
        List<Map<String, Object>> list = userMapper.selectPage(pageSize, page - 1);
        long total = userMapper.selectUserList2Count();
        return R.ok("获取数据成功", PageResult.of(list, total, pageSize, page));
    }

    @Override
    public R<Void> updatePassword(PasswordUpdateDto dto) {
        String username = currentUsername();
        UserEntity user = userMapper.selectUserByUsername(username);
        if (user == null) {
            throw new BizException("获取用户信息失败");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("密码不正确");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BizException("密码不能和之前一样");
        }
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("password", passwordEncoder.encode(dto.getNewPassword()));
        updateWrapper.set("update_time", new Timestamp(System.currentTimeMillis()));
        int rows = userMapper.update(null, updateWrapper);
        return R.ok(rows > 0 ? "更新成功" : "更新失败", null);
    }

    @Override
    public R<Void> resetPassword(ResetPasswordDto dto) {
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", dto.getUsername());
        updateWrapper.set("password", passwordEncoder.encode(dto.getPassword()));
        updateWrapper.set("update_time", new Timestamp(System.currentTimeMillis()));
        int rows = userMapper.update(null, updateWrapper);
        return R.ok(rows > 0 ? "重置成功" : "重置失败", null);
    }

    @Override
    @Transactional
    public R<Void> updateUser(UserUpdateDto dto) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "user:write".equals(authority.getAuthority()));
        if (!isAdmin) {
            // 非管理员只允许修改本人资料，且不能变更角色
            UserEntity self = userMapper.selectUserByUsername(currentUsername());
            if (self == null || self.getId() != dto.getId().intValue()) {
                throw new BizException("只能修改本人信息");
            }
        }

        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", dto.getId())
                .set("username", dto.getUsername())
                .set("name", dto.getName())
                .set("email", dto.getEmail())
                .set("mobile", dto.getMobile())
                .set("update_time", new Timestamp(System.currentTimeMillis()));
        int updateResult = userMapper.update(null, updateWrapper);
        if (updateResult <= 0) {
            throw new BizException("更新用户信息失败");
        }

        // 只在显式传入role_codes时重建角色关联；null表示本次不修改角色（如个人中心改资料）
        if (isAdmin && dto.getRoleCodes() != null) {
            List<String> roleCodes = dto.getRoleCodes().stream()
                    .filter(code -> code != null && !code.isBlank())
                    .toList();
            userMapper.deleteUserRoles(dto.getId());
            if (!roleCodes.isEmpty()) {
                List<Map<String, Object>> roles = userMapper.selectRoleIdsByCodes(roleCodes);
                roles.forEach(role -> {
                    role.put("user_id", dto.getId());
                    role.put("role_id", ((Number) role.get("id")).longValue());
                });
                userMapper.insertUserRoles(roles);
            }
        }
        return R.ok("更新成功", null);
    }

    @Override
    public R<Void> updateAvatar(Long userId, MultipartFile avatar) throws IOException {
        String remotePath = "/test/" + fileUtils.generateFilePath(avatar);
        ftpService.uploadFile(remotePath, avatar);
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .set("avatar", remotePath)
                .set("update_time", new Timestamp(System.currentTimeMillis()));
        int updateResult = userMapper.update(null, updateWrapper);
        return R.ok(updateResult > 0 ? "更新成功" : "更新失败", null);
    }

    @Override
    public R<UserVO> getUser(String username) {
        //脱敏后返回，不携带密码
        return R.ok(UserVO.from(userMapper.selectUserByUsername(username)));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BizException("获取用户信息失败");
        }
        return authentication.getName();
    }
}
