package com.pig4cloud.dept.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.dept.entity.DeptEntity;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限：按当前用户角色的data_scope计算用户/业务数据的可见范围。
 * 1=本租户全部（不限制）；2=本部门及以下；3=仅本人。super角色恒不限制。
 * 首个落地场景是用户列表，后续业务表查询可复用resolveCurrentUser()。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    public static final String SCOPE_ALL = "1";
    public static final String SCOPE_DEPT = "2";
    public static final String SCOPE_SELF = "3";
    public static final String SCOPE_CUSTOM = "4";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final DeptService deptService;

    public DataScopeFilter resolveCurrentUser() {
        if (isSuper()) {
            return DataScopeFilter.all();
        }
        String username = currentUsername();
        UserEntity user = userMapper.selectUserByUsername(username);
        List<RoleEntity> roles = roleMapper.selectRolesByUsername(username);
        if (roles == null || roles.isEmpty()) {
            // 无角色用户只能看自己
            return user == null ? DataScopeFilter.all()
                    : new DataScopeFilter(null, user.getId().longValue());
        }
        boolean hasAllScope = roles.stream().anyMatch(role -> SCOPE_ALL.equals(role.getData_scope()));
        if (hasAllScope) {
            return DataScopeFilter.all();
        }
        Set<Integer> deptIds = new HashSet<>();
        boolean includeSelf = false;
        for (RoleEntity role : roles) {
            if (SCOPE_DEPT.equals(role.getData_scope()) && user != null) {
                deptIds.addAll(deptService.selfAndDescendantIds(user.getDept_id()));
            } else if (SCOPE_CUSTOM.equals(role.getData_scope()) && StringUtils.hasText(role.getCustom_dept_ids())) {
                // 自定义部门集：仅可见勾选的部门本身（不含其下级，精确到勾选项）
                for (String id : role.getCustom_dept_ids().split(",")) {
                    try {
                        deptIds.add(Integer.valueOf(id.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (SCOPE_SELF.equals(role.getData_scope())) {
                includeSelf = true;
            }
        }
        Long selfId = includeSelf && user != null ? user.getId().longValue() : null;
        return new DataScopeFilter(deptIds, selfId);
    }

    private boolean isSuper() {
        List<GrantedAuthority> authorities = List.of();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            authorities = List.copyOf(authentication.getAuthorities());
        }
        return authorities.stream().anyMatch(authority -> "super".equals(authority.getAuthority()));
    }

    private String currentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BizException("获取用户信息失败");
        }
        return authentication.getName();
    }
}
