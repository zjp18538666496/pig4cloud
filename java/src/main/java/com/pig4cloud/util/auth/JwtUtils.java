package com.pig4cloud.util.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private final JwtProperties jwtProperties;

    /**
     * 生成令牌
     */
    public String getJwt(Map<String, Object> claims) {
        Key signingKey = jwtProperties.getSigningKey();
        Long expire = jwtProperties.getExpire();
        claims.put("token_type", "access");
        return Jwts.builder()
                .setClaims(claims) //设置载荷内容
                .signWith(SignatureAlgorithm.HS256, signingKey) //设置签名算法
                .setExpiration(new Date(System.currentTimeMillis() + expire)) //设置有效时间
                .compact();
    }

    /**
     * 解析令牌
     */
    public Claims parseJwt(String jwt) {
        Key signingKey = jwtProperties.getSigningKey();
        Claims claims = Jwts.parser()
                .setSigningKey(signingKey) //指定签名密钥
                .build()
                .parseClaimsJws(jwt) //开始解析令牌
                .getBody();
        if (!"access".equals(claims.get("token_type"))) {
            throw new IllegalArgumentException("无效的token");
        }

        return claims;
    }

    /**
     * 生成长期令牌
     */
    public String getRefreshToken(Map<String, Object> claims) {
        Key signingKey = jwtProperties.getSigningKey();
        Long refreshExpire = jwtProperties.getRefreshExpire();
        claims.put("token_type", "refresh");
        return Jwts.builder()
                .setClaims(claims) //设置载荷内容
                .signWith(SignatureAlgorithm.HS256, signingKey) //设置签名算法
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpire)) //设置长期有效时间
                .compact();
    }

    /**
     * 解析长期令牌
     */
    public Claims parseRefreshToken(String refreshToken) {
        Key signingKey = jwtProperties.getSigningKey();
        Claims claims = Jwts.parser()
                .setSigningKey(signingKey) //指定签名密钥
                .build()
                .parseClaimsJws(refreshToken) //开始解析令牌
                .getBody();

        if (!"refresh".equals(claims.get("token_type"))) {
            throw new IllegalArgumentException("无效的token");
        }
        return claims;
    }

    /**
     * 刷新token
     */
    public String refreshToken(String token) {
        Claims claims = parseRefreshToken(token);
        String username = (String) claims.get("username");
        String authorityString = (String) claims.get("authorityString");
        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put("username", username);
        newClaims.put("authorityString", authorityString);
        return getJwt(newClaims);
    }
}
