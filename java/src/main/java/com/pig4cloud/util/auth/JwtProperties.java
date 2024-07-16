package com.pig4cloud.util.auth;

import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;

/**
 * 配置JWT的密钥和过期时间
 */
@Component
public class JwtProperties {
    public Key getSigningKey() {
        // 使用 Base64 解码并生成密钥
        String secret = "fpDZ3ec66c9vtQGst6Nt3TgLZ91IUk6eYmKcgRUeafE=";
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(decodedKey);
    }


    /**
     * 令牌过期时间
     */
    public Long getExpire() {
        return  60 * 60 *1000L;
    }

    /**
     * 长期令牌过期时间
     */
    public Long getRefreshExpire() {
        return 30 * 24 * 60 * 60 *1000L;
    }
}
