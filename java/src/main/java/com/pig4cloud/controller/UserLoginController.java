package com.pig4cloud.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.common.result.R;
import com.pig4cloud.entity.UserDetailsEntity;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import com.pig4cloud.user.vo.UserVO;
import com.pig4cloud.util.auth.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public R<UserVO> doLogin(@RequestBody UserDetailsEntity userDetailsEntity, HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetailsEntity.getUsername(), userDetailsEntity.getPassword());
            Authentication authentication = authenticationManager.authenticate(auth);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            //更新用户最后登录时间
            UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("username", userDetailsEntity.getUsername())
                    .set("last_login_time", new Timestamp(System.currentTimeMillis()));
            userMapper.update(null, updateWrapper);

            //获取用户权限信息
            String authorityString = "";
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                authorityString = authority.getAuthority();
            }

            //用户身份验证成功，生成并返回jwt令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", userDetails.getUsername());
            claims.put("authorityString", authorityString);
            String jwtToken = jwtUtils.getJwt(claims);
            String refreshToken = jwtUtils.getRefreshToken(claims);

            //token通过响应头下发，前端从header读取
            response.setHeader("Refresh-Token", refreshToken);
            response.setHeader("Authorization", "Bearer " + jwtToken);

            //脱敏后返回用户信息，不携带密码
            return R.ok("请求成功", UserVO.from(userMapper.selectUserByUsername(userDetailsEntity.getUsername())));
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            //用户身份验证失败，返回登陆失败提示（不区分账号或密码错误，避免枚举探测）
            return R.fail("用户名或密码不正确");
        } catch (Exception ex) {
            return R.fail("登录失败，请稍后重试");
        }
    }
}
