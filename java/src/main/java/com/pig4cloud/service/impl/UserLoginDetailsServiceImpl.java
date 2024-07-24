package com.pig4cloud.service.impl;

import com.pig4cloud.dao.AuthorityMapper;
import com.pig4cloud.dao.UserMapper;
import com.pig4cloud.entity.AuthorityEntity;
import com.pig4cloud.entity.UserDetailsEntity;
import com.pig4cloud.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Component
public class UserLoginDetailsServiceImpl implements UserDetailsService {
    private final UserMapper userMapper;
    @Autowired
    private final AuthorityMapper authorityMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.selectUserByUsername(username);
//        List<AuthorityEntity> authorities = authorityMapper.selectAuthorityByUsername(username);
        List<AuthorityEntity> authorities = List.of(
                new AuthorityEntity(1, "login", "登录权限"),
                new AuthorityEntity(2, "root", "管理员")
        );
        StringJoiner stringJoiner = new StringJoiner(",", "", "");
        authorities.forEach(authority -> stringJoiner.add(authority.getName()));
        return new UserDetailsEntity(userEntity.getUsername(), userEntity.getPassword(),
                AuthorityUtils.commaSeparatedStringToAuthorityList(stringJoiner.toString())
        );
    }
}
