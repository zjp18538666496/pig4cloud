package com.pig4cloud.auth.service;

import com.pig4cloud.auth.UserPrincipal;
import com.pig4cloud.auth.entity.AuthorityEntity;
import com.pig4cloud.auth.mapper.AuthorityMapper;
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

import java.util.List;
import java.util.StringJoiner;

/**
 * 从数据库加载用户与角色权限，供DaoAuthenticationProvider认证使用
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final AuthorityMapper authorityMapper;
    private final TenantMapper tenantMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.selectUserByUsername(username);
        if (userEntity == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }
        // 所属租户被禁用时拒绝登录（平台账号tenant_id=0无租户记录，跳过）
        if (userEntity.getTenant_id() != null && userEntity.getTenant_id() != 0) {
            TenantEntity tenant = tenantMapper.selectById(userEntity.getTenant_id());
            if (tenant == null || !"1".equals(tenant.getStatus())) {
                throw new DisabledException("所属租户已停用");
            }
        }

        List<AuthorityEntity> authorities = authorityMapper.selectAuthorityByUsername(username);
        StringJoiner stringJoiner = new StringJoiner(",", "", "");
        if (authorities != null) {
            authorities.forEach(authority -> stringJoiner.add(authority.getName()));
        }

        return new UserPrincipal(
                userEntity.getUsername(),
                userEntity.getPassword(),
                AuthorityUtils.commaSeparatedStringToAuthorityList(stringJoiner.toString()));
    }
}
