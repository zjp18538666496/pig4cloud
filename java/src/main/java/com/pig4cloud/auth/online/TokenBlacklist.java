package com.pig4cloud.auth.online;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * token黑名单（内存实现，单机有效）：登出/强退/改密后将jti拉黑至其自然过期时间，
 * JwtAuthenticationFilter每次请求校验。重启即清空，多实例部署需换Redis等共享存储。
 */
@Component
public class TokenBlacklist {

    /**
     * jti -> token过期时间戳(毫秒)，过期后条目即无意义，由定时任务清理
     */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void revoke(String jti, long expireAtMillis) {
        if (jti != null && !jti.isBlank() && expireAtMillis > System.currentTimeMillis()) {
            blacklist.put(jti, expireAtMillis);
        }
    }

    public boolean isRevoked(String jti) {
        return jti != null && blacklist.containsKey(jti);
    }

    public void cleanExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
