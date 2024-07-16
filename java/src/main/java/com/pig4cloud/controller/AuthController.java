package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.util.auth.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;

    @PostMapping("/refresh-token")
    public Response refreshToken(@RequestParam String refreshToken, HttpServletResponse response) {
        try {
            // 验证 refresh token 的有效性
            Claims claims = jwtUtils.parseRefreshToken(refreshToken);
            // 生成新的 access token
            String newAccessToken = jwtUtils.getJwt(claims);
            // 在响应头中设置token
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            return new ResponseImpl(200, "请求成功", null);
        } catch (Exception e) {
            return new ResponseImpl(401, "刷新token无效", e.getMessage());
        }
    }
}
