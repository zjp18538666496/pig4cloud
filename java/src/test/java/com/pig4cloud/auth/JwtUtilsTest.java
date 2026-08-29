package com.pig4cloud.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = createJwtUtils(60_000L);
    }

    private JwtUtils createJwtUtils(long expireMillis) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpire(expireMillis);
        properties.setRefreshExpire(3_600_000L);
        return new JwtUtils(properties);
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", "root");
        claims.put("authorityString", "root,login");
        return claims;
    }

    @Test
    void parseJwtRoundTrip() {
        String token = jwtUtils.getJwt(claims());

        Claims parsed = jwtUtils.parseJwt(token);
        assertEquals("root", parsed.get("username"));
        assertEquals("root,login", parsed.get("authorityString"));
    }

    @Test
    void accessAndRefreshTokenAreDistinguishable() {
        String access = jwtUtils.getJwt(new HashMap<>(claims()));
        String refresh = jwtUtils.getRefreshToken(new HashMap<>(claims()));

        // token_type不匹配时拒绝解析，防止refresh token被当作access token使用
        assertThrows(IllegalArgumentException.class, () -> jwtUtils.parseJwt(refresh));
        assertThrows(IllegalArgumentException.class, () -> jwtUtils.parseRefreshToken(access));

        assertTrue(jwtUtils.refreshToken(refresh).startsWith("ey"));
    }

    @Test
    void expiredTokenRejected() {
        JwtUtils expiredUtils = createJwtUtils(-1000L);
        String token = expiredUtils.getJwt(claims());

        assertThrows(ExpiredJwtException.class, () -> expiredUtils.parseJwt(token));
    }

    @Test
    void tamperedSignatureRejected() {
        String token = jwtUtils.getJwt(claims());
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwtUtils.parseJwt(tampered));
    }
}
