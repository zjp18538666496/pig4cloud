package com.pig4cloud.util.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
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
        return Jwts.parser()
                .setSigningKey(signingKey) //指定签名密钥
                .build()
                .parseClaimsJws(jwt) //开始解析令牌
                .getBody();
    }
}
