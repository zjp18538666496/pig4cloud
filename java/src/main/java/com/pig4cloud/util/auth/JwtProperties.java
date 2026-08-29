package com.pig4cloud.util.auth;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * JWT配置：密钥与过期时间，密钥从配置文件或环境变量JWT_SECRET注入，禁止硬编码
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Base64编码的HS256签名密钥（至少32字节）
     */
    private String secret;

    /**
     * 访问令牌过期时间（毫秒），默认1小时
     */
    private long expire = 60 * 60 * 1000L;

    /**
     * 刷新令牌过期时间（毫秒），默认30天
     */
    private long refreshExpire = 30 * 24 * 60 * 60 * 1000L;

    public SecretKey getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret未配置，请通过环境变量JWT_SECRET或application-local.yaml提供");
        }
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(decodedKey);
    }
}
