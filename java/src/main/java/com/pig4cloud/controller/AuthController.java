package com.pig4cloud.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.util.auth.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;

    /**
     * 刷新访问令牌：用长期refresh token换取新的access token
     */
    @PostMapping("/refresh-token")
    public R<Void> refreshToken(@RequestBody Map<String, Object> map, HttpServletResponse response) {
        try {
            String refreshToken = (String) map.get("refreshToken");
            String newAccessToken = jwtUtils.refreshToken(refreshToken);
            //token通过响应头下发，前端从header读取
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            return R.ok("请求成功", null);
        } catch (Exception e) {
            return R.fail(401, "刷新token无效");
        }
    }
}
