package com.pig4cloud.auth.controller;

import com.pig4cloud.auth.dto.LoginRequest;
import com.pig4cloud.auth.dto.LoginResult;
import com.pig4cloud.auth.dto.RefreshTokenRequest;
import com.pig4cloud.auth.service.AuthService;
import com.pig4cloud.auth.JwtUtils;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.user.vo.UserVO;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    /**
     * 账号密码登录，token通过响应头Authorization/Refresh-Token下发
     */
    @PostMapping("/login")
    @LogRecord(module = "认证", operation = "用户登录")
    public R<UserVO> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request);
        response.setHeader("Authorization", "Bearer " + result.accessToken());
        response.setHeader("Refresh-Token", result.refreshToken());
        return R.ok("请求成功", result.user());
    }

    /**
     * 用长期refresh token换取新的access token
     */
    @PostMapping("/refresh-token")
    @LogRecord(module = "认证", operation = "刷新令牌")
    public R<Void> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletResponse response) {
        try {
            String newAccessToken = jwtUtils.refreshToken(request.getRefreshToken());
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            return R.ok("请求成功", null);
        } catch (JwtException | IllegalArgumentException e) {
            // 返回code=401，前端据此清理本地缓存并引导重新登录
            return R.fail(R.UNAUTHORIZED, "刷新token无效");
        }
    }
}
