package com.pig4cloud.auth.service;

import com.pig4cloud.auth.JwtUtils;
import com.pig4cloud.auth.dto.LoginRequest;
import com.pig4cloud.auth.dto.LoginResult;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import com.pig4cloud.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Override
    public LoginResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 角色编码+按钮权限点合并存入token，解析端再拆开
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        String authorityString = String.join(",", authorities);

        UserEntity user = userMapper.selectUserByUsername(request.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", authentication.getName());
        claims.put("authorityString", authorityString);
        claims.put("tenantId", user == null ? 0 : user.getTenant_id());

        UserVO userVO = UserVO.from(user);
        if (userVO != null) {
            // 权限点随登录响应下发，前端v-permission据此控制按钮
            userVO.setPermissions(authorities);
        }
        return new LoginResult(jwtUtils.getJwt(claims), jwtUtils.getRefreshToken(claims), userVO);
    }
}
