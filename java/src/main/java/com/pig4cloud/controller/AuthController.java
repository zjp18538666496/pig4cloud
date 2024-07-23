package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.util.auth.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;

    /**
     * 刷新Token
     */
    @PostMapping("/refresh-token")
    public Response refreshToken(@RequestBody Map<String, Object> map, HttpServletResponse response) {
        try {
            String refreshToken = (String) map.get("refreshToken");
            String newAccessToken = jwtUtils.refreshToken(refreshToken);
            // 在响应头中设置token和刷新token
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            // 使用URLEncoder来确保Cookie值中没有非法字符
            String encodedJwtToken = URLEncoder.encode(newAccessToken, StandardCharsets.UTF_8);
            // 创建cookie对象
            Cookie tokenCookie = new Cookie("Authorization", encodedJwtToken);
            // 设置cookie属性
            tokenCookie.setHttpOnly(true);
            tokenCookie.setSecure(false);
            tokenCookie.setPath("/"); // 设置路径为根路径
            // 将cookie添加到响应中
            response.addCookie(tokenCookie);
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            return new ResponseImpl(200, "请求成功", null);
        } catch (Exception e) {
            return new ResponseImpl(401, "刷新token无效", e.getMessage());
        }
    }
}
