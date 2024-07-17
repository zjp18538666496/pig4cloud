package com.pig4cloud.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.UserMapper;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.entity.UserDetailsEntity;
import com.pig4cloud.entity.UserEntity;
import com.pig4cloud.util.auth.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user/login")
@RequiredArgsConstructor
public class UserLoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @PostMapping
    public Response doLogin(@RequestBody UserDetailsEntity userDetailsEntity, HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetailsEntity.getUsername(), userDetailsEntity.getPassword());
            Authentication authentication = authenticationManager.authenticate(auth);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("username", userDetailsEntity.getUsername())
                    .set("last_login_time", new Timestamp(System.currentTimeMillis()));
            //更新用户最后登录时间
            userMapper.update(null, updateWrapper);

            //获取用户权限信息
            String authorityString = "";
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                authorityString = authority.getAuthority();
            }
            UserEntity userEntity = userMapper.selectUserByUsername(userDetailsEntity.getUsername());

            //用户身份验证成功，生成并返回jwt令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", userDetails.getUsername());
            claims.put("authorityString", authorityString);
            String jwtToken = jwtUtils.getJwt(claims);
            String refreshToken = jwtUtils.getRefreshToken(claims);

            // 在响应头中设置token和刷新token
            response.setHeader("Authorization", "Bearer " + jwtToken);
            response.setHeader("Refresh-Token", refreshToken);

            // 使用URLEncoder来确保Cookie值中没有非法字符
            String encodedJwtToken = URLEncoder.encode(jwtToken, StandardCharsets.UTF_8);
            String encodedRefreshToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

            // 创建cookie对象
            Cookie tokenCookie = new Cookie("Authorization", encodedJwtToken);
            Cookie refreshTokenCookie = new Cookie("Refresh-Token", encodedRefreshToken);

            // 设置cookie属性
            tokenCookie.setHttpOnly(false);
            tokenCookie.setSecure(false);
            tokenCookie.setPath("/"); // 设置路径为根路径
//            tokenCookie.setMaxAge(3600); // 1小时

            refreshTokenCookie.setHttpOnly(false);
            refreshTokenCookie.setSecure(false);
            refreshTokenCookie.setPath("/");
//            refreshTokenCookie.setMaxAge(86400); // 1天

            // 将cookie添加到响应中
            response.addCookie(tokenCookie);
            response.addCookie(refreshTokenCookie);


            return new ResponseImpl(200, "请求成功", userEntity);
        } catch (Exception ex) {
            //用户身份验证失败，返回登陆失败提示
            return new ResponseImpl(-200, "用户名或密码不正确", ex.toString());
        }
    }
}
