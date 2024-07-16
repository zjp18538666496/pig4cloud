package com.pig4cloud.controller;

import com.pig4cloud.util.auth.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            // 验证 refresh token 的有效性
            Claims claims = jwtUtils.parseRefreshToken(refreshToken);

            // 生成新的 access token
            String newAccessToken = jwtUtils.getJwt(claims);
            return ResponseEntity.ok(newAccessToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }
}
