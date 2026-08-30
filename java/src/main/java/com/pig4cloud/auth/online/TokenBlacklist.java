package com.pig4cloud.auth.online;

import com.pig4cloud.common.store.StateStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * token黑名单：登出/强退/改密后将jti拉黑至其自然过期时间，JwtAuthenticationFilter每次请求校验。
 * 状态走StateStore（memory单机/redis多实例），键TTL即token剩余有效期，过期自动出黑名单
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StateStore stateStore;

    public void revoke(String jti, long expireAtMillis) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long ttl = expireAtMillis - System.currentTimeMillis();
        if (ttl > 0) {
            stateStore.put(KEY_PREFIX + jti, "1", ttl);
        }
    }

    public boolean isRevoked(String jti) {
        return jti != null && stateStore.exists(KEY_PREFIX + jti);
    }
}
