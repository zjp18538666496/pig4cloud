package com.pig4cloud.auth.service;

import com.pig4cloud.auth.UserPrincipal;
import com.pig4cloud.auth.entity.AuthorityEntity;
import com.pig4cloud.auth.mapper.AuthorityMapper;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.role.service.RoleHierarchyService;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 从数据库加载用户与角色权限，供DaoAuthenticationProvider认证使用。
 * 权限点两部分：角色编码取自身角色（不继承，防止子角色挂到super下越权）；
 * 按钮/菜单权限标识沿父链继承（自身角色+全部祖先角色）。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AuthorityMapper authorityMapper;
    private final RoleHierarchyService roleHierarchyService;
    private final TenantMapper tenantMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.selectUserByUsername(username);
        if (userEntity == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }
        // 所属租户被禁用/已过期时拒绝登录（平台账号tenant_id=0无租户记录，跳过）
        if (userEntity.getTenant_id() != null && userEntity.getTenant_id() != 0) {
            TenantEntity tenant = tenantMapper.selectById(userEntity.getTenant_id());
            if (tenant == null || !"1".equals(tenant.getStatus())) {
                throw new DisabledException("所属租户已停用");
            }
            if (tenant.getExpire_time() != null && tenant.getExpire_time().before(new Date())) {
                throw new DisabledException("所属租户已过期，请联系平台管理员续期");
            }
        }

        // 角色编码：仅自身角色
        List<RoleEntity> userRoles = roleMapper.selectRolesByUsername(username);
        Map<String, AuthorityEntity> authorityByName = new LinkedHashMap<>();
        if (userRoles != null) {
            for (RoleEntity role : userRoles) {
                authorityByName.put(role.getRole_code(),
                        new AuthorityEntity(role.getId(), role.getRole_code(), role.getRole_name()));
            }
        }
        // 按钮/菜单权限标识：自身角色+全部祖先角色（子角色沿父链继承）
        List<Integer> effectiveRoleIds = roleHierarchyService.effectiveRoleIds(userRoles);
        List<AuthorityEntity> perms = effectiveRoleIds.isEmpty()
                ? new ArrayList<>() : authorityMapper.selectPermsByRoleIds(effectiveRoleIds);
        for (AuthorityEntity perm : perms) {
            authorityByName.putIfAbsent(perm.getName(), perm);
        }
        StringJoiner stringJoiner = new StringJoiner(",", "", "");
        authorityByName.values().forEach(authority -> stringJoiner.add(authority.getName()));

        return new UserPrincipal(
                userEntity.getUsername(),
                userEntity.getPassword(),
                AuthorityUtils.commaSeparatedStringToAuthorityList(stringJoiner.toString()));
    }
}
