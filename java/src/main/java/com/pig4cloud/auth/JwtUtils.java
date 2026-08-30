package com.pig4cloud.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private static final String TOKEN_TYPE = "token_type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    /**
     * 生成访问令牌
     */
    public String getJwt(Map<String, Object> claims) {
        return buildToken(claims, jwtProperties.getExpire(), TYPE_ACCESS);
    }

    /**
     * 生成刷新令牌
     */
    public String getRefreshToken(Map<String, Object> claims) {
        return buildToken(claims, jwtProperties.getRefreshExpire(), TYPE_REFRESH);
    }

    /**
     * 解析访问令牌
     */
    public Claims parseJwt(String jwt) {
        return parse(jwt, TYPE_ACCESS);
    }

    /**
     * 解析刷新令牌
     */
    public Claims parseRefreshToken(String refreshToken) {
        return parse(refreshToken, TYPE_REFRESH);
    }

    /**
     * 用刷新令牌换取新的访问令牌。
     * 注意必须完整复制业务claims（含tenantId），否则刷新后租户上下文丢失、租户拦截器失效
     */
    public String refreshToken(String token) {
        Claims claims = parseRefreshToken(token);
        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put("username", claims.get("username"));
        newClaims.put("authorityString", claims.get("authorityString"));
        newClaims.put("tenantId", claims.get("tenantId"));
        return getJwt(newClaims);
    }

    private String buildToken(Map<String, Object> claims, long ttlMillis, String tokenType) {
        Map<String, Object> payload = new HashMap<>(claims);
        payload.put(TOKEN_TYPE, tokenType);
        return Jwts.builder()
                .claims(payload)
                // jti唯一标识：在线会话注册与登出/强退黑名单都靠它定位token
                .id(UUID.randomUUID().toString())
                .signWith(jwtProperties.getSigningKey(), Jwts.SIG.HS256)
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .compact();
    }

    private Claims parse(String jwt, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtProperties.getSigningKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
        if (!expectedType.equals(claims.get(TOKEN_TYPE))) {
            throw new IllegalArgumentException("无效的token");
        }
        return claims;
    }
}
