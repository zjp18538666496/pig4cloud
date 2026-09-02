package com.pig4cloud.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.dept.service.DataScopeFilter;
import com.pig4cloud.dept.service.DataScopeService;
import com.pig4cloud.dept.service.DeptService;
import com.pig4cloud.auth.online.SessionKickService;
import com.pig4cloud.auth.service.PasswordPolicyService;
import com.pig4cloud.file.service.FtpService;
import com.pig4cloud.file.util.FileUtils;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final TenantMapper tenantMapper;
    private final DataScopeService dataScopeService;
    private final DeptService deptService;
    private final SessionKickService sessionKickService;
    private final PasswordPolicyService passwordPolicyService;
    private final com.pig4cloud.config.service.ConfigService configService;
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
        // 密码策略校验（最小长度/复杂度走sys_config）
        passwordPolicyService.validate(dto.getPassword());
        // 租户归属由服务端决定：超管可指定目标租户；未认证上下文(注册)归默认租户1，其余取当前登录用户租户
        Integer tenantId;
        if (dto.getTenantId() != null) {
            if (!UserContext.isSuperTenant()) {
                throw new BizException("仅平台管理员可指定用户所属租户");
            }
            if (tenantMapper.selectById(dto.getTenantId()) == null) {
                throw new BizException("目标租户不存在");
            }
            tenantId = dto.getTenantId();
        } else {
            tenantId = UserContext.getTenantId() == null ? 1 : UserContext.getTenantId();
        }
        // 租户用户数配额校验
        TenantEntity tenant = tenantMapper.selectById(tenantId);
        if (tenant != null && tenant.getUser_limit() != null) {
            Long count = userMapper.selectCount(new QueryWrapper<UserEntity>().eq("tenant_id", tenantId));
            if (count >= tenant.getUser_limit()) {
                throw new BizException("租户用户数已达配额上限(" + tenant.getUser_limit() + ")，无法新增用户");
            }
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(dto.getUsername());
        userEntity.setPassword(passwordEncoder.encode(dto.getPassword()));
        userEntity.setName(dto.getName() == null || dto.getName().isBlank() ? dto.getUsername() : dto.getName());
        userEntity.setMobile(dto.getMobile());
        userEntity.setEmail(dto.getEmail());
        userEntity.setDept_id(dto.getDeptId());
        userEntity.setCreate_time(new Timestamp(System.currentTimeMillis()));
        userEntity.setTenant_id(tenantId);
        int rows = userMapper.insert(userEntity);
        if (rows > 0 && dto.getPostIds() != null && !dto.getPostIds().isEmpty()) {
            bindPosts(userEntity.getId().longValue(), dto.getPostIds());
        }
        return R.ok(rows > 0 ? "注册成功" : "注册失败", null);
    }

    /**
     * 重建用户岗位关联
     */
    private void bindPosts(Long userId, List<Integer> postIds) {
        userMapper.deleteUserPosts(userId);
        List<Map<String, Object>> posts = postIds.stream().map(postId -> {
            Map<String, Object> post = new HashMap<>();
            post.put("user_id", userId);
            post.put("post_id", postId);
            return post;
        }).toList();
        userMapper.insertUserPosts(posts);
    }

    @Override
    public R<Void> deleteUser(UserDeleteDto dto) {
        // 删除前踢掉该用户全部在线会话
        sessionKickService.kickUser(dto.getUsername());
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", dto.getUsername());
        int rows = userMapper.delete(queryWrapper);
        return R.ok(rows > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    public R<PageResult<Map<String, Object>>> getUserLists(UserDto userDto) {
        long page = userDto.getPage();
        long pageSize = userDto.getPageSize();
        // 数据权限：按当前用户角色的data_scope过滤可见范围
        DataScopeFilter filter = dataScopeService.resolveCurrentUser();
        // 部门筛选：展开为含子部门的id集合；租户筛选仅超管生效（普通用户被租户拦截器限制在本租户）
        Collection<Integer> filterDeptIds = userDto.getDeptId() == null
                ? null : deptService.selfAndDescendantIds(userDto.getDeptId());
        Integer tenantId = UserContext.isSuperTenant() ? userDto.getTenantId() : null;
        List<Map<String, Object>> list = userMapper.selectPage(pageSize, page - 1,
                filter.deptIds(), filter.selfId(), userDto.getUsername(), filterDeptIds, tenantId);
        long total = userMapper.selectUserList2Count(
                filter.deptIds(), filter.selfId(), userDto.getUsername(), filterDeptIds, tenantId);
        // 脱敏开关：无user:write权限者看到的手机号/邮箱为脱敏形式
        if (maskEnabledForViewer()) {
            list.forEach(this::maskRow);
        }
        return R.ok("获取数据成功", PageResult.of(list, total, pageSize, page));
    }

    private boolean maskEnabledForViewer() {
        boolean hasUserWrite = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "user:write".equals(authority.getAuthority()));
        return configService.getBool("mask.enabled", true) && !hasUserWrite;
    }

    private void maskRow(Map<String, Object> row) {
        Object mobile = row.get("mobile");
        if (mobile instanceof String value && !value.isBlank()) {
            row.put("mobile", com.pig4cloud.common.util.DesensitizeUtil.maskMobile(value));
        }
        Object email = row.get("email");
        if (email instanceof String value && !value.isBlank()) {
            row.put("email", com.pig4cloud.common.util.DesensitizeUtil.maskEmail(value));
        }
    }

    @Override
    public List<Map<String, Object>> exportRows(UserDto userDto) {
        // 与列表同口径（筛选+数据权限），分页拉全量，上限1万行
        DataScopeFilter filter = dataScopeService.resolveCurrentUser();
        Collection<Integer> filterDeptIds = userDto.getDeptId() == null
                ? null : deptService.selfAndDescendantIds(userDto.getDeptId());
        Integer tenantId = UserContext.isSuperTenant() ? userDto.getTenantId() : null;
        List<Map<String, Object>> all = new ArrayList<>();
        long page = 0;
        while (all.size() < 10000) {
            List<Map<String, Object>> list = userMapper.selectPage(1000, page,
                    filter.deptIds(), filter.selfId(), userDto.getUsername(), filterDeptIds, tenantId);
            if (list.isEmpty()) {
                break;
            }
            all.addAll(list);
            if (list.size() < 1000) {
                break;
            }
            page++;
        }
        return all;
    }

    @Override
    public Map<String, Object> importUsers(List<UserCreateDto> rows) {
        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();
        int index = 1;
        for (UserCreateDto row : rows == null ? List.<UserCreateDto>of() : rows) {
            try {
                createUser(row);
                success++;
            } catch (Exception ex) {
                Map<String, Object> failure = new HashMap<>();
                failure.put("row", index);
                failure.put("username", row == null ? "" : row.getUsername());
                failure.put("reason", ex instanceof BizException ? ex.getMessage() : "系统异常");
                failures.add(failure);
            }
            index++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success);
        result.put("failures", failures);
        return result;
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
        // 新密码走密码策略校验
        passwordPolicyService.validate(dto.getNewPassword());
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("password", passwordEncoder.encode(dto.getNewPassword()));
        updateWrapper.set("pwd_update_time", new Timestamp(System.currentTimeMillis()));
        updateWrapper.set("force_pwd_change", 0);
        updateWrapper.set("update_time", new Timestamp(System.currentTimeMillis()));
        int rows = userMapper.update(null, updateWrapper);
        return R.ok(rows > 0 ? "更新成功" : "更新失败", null);
    }

    @Override
    public R<Void> resetPassword(ResetPasswordDto dto) {
        // 管理员重置：走密码策略校验；标记首登强制改密（可配置）并踢掉该用户在线会话
        passwordPolicyService.validate(dto.getPassword());
        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", dto.getUsername());
        updateWrapper.set("password", passwordEncoder.encode(dto.getPassword()));
        updateWrapper.set("pwd_update_time", new Timestamp(System.currentTimeMillis()));
        updateWrapper.set("force_pwd_change", passwordPolicyService.forceChangeOnReset() ? 1 : 0);
        updateWrapper.set("update_time", new Timestamp(System.currentTimeMillis()));
        int rows = userMapper.update(null, updateWrapper);
        sessionKickService.kickUser(dto.getUsername());
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
        // 管理员可调整部门归属（null表示清空）；非管理员不允许改动
        if (isAdmin) {
            updateWrapper.set("dept_id", dto.getDeptId());
        }
        int updateResult = userMapper.update(null, updateWrapper);
        if (updateResult <= 0) {
            throw new BizException("更新用户信息失败");
        }

        // 管理员可调整岗位（post_ids非null即重建，空数组清空）；非管理员不允许改动
        if (isAdmin && dto.getPostIds() != null) {
            bindPosts(dto.getId(), dto.getPostIds());
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
            // 角色变更后踢掉该用户在线会话，权限立即生效
            sessionKickService.kickUser(dto.getUsername());
        }
        return R.ok("更新成功", null);
    }

    @Override
    public R<Void> updateAvatar(Long userId, MultipartFile avatar) throws IOException {
        // 存完整FTP文件路径（目录+原文件名）：头像公开接口按此路径直接流式返回
        String remotePath = "/test/" + fileUtils.generateFilePath(avatar) + avatar.getOriginalFilename();
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
        UserVO vo = UserVO.from(userMapper.selectUserByUsername(username));
        if (vo != null && maskEnabledForViewer()) {
            vo.setMobile(com.pig4cloud.common.util.DesensitizeUtil.maskMobile(vo.getMobile()));
            vo.setEmail(com.pig4cloud.common.util.DesensitizeUtil.maskEmail(vo.getEmail()));
        }
        return R.ok(vo);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BizException("获取用户信息失败");
        }
        return authentication.getName();
    }
}
