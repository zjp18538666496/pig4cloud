package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定（内存实现，单机有效）：同一用户名连续失败达到上限后临时锁定，
 * 登录成功即解除。重启清零，多实例部署需换共享存储。
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 登录前检查：锁定中则直接拒绝
     */
    public void checkLocked(String username) {
        Attempt attempt = attempts.get(username);
        if (attempt == null) {
            return;
        }
        if (attempt.lockUntil > System.currentTimeMillis()) {
            long remainMinutes = (attempt.lockUntil - System.currentTimeMillis()) / 60000 + 1;
            throw new BizException("登录失败次数过多，账号已锁定，请约" + remainMinutes + "分钟后重试");
        }
    }

    public void recordFailure(String username) {
        attempts.compute(username, (key, attempt) -> {
            int failCount = (attempt == null || attempt.lockUntil <= System.currentTimeMillis())
                    ? 1 : attempt.failCount + 1;
            long lockUntil = failCount >= MAX_ATTEMPTS
                    ? System.currentTimeMillis() + LOCK_MILLIS : 0L;
            return new Attempt(failCount, lockUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    public void cleanExpired() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry ->
                entry.getValue().lockUntil > 0 && entry.getValue().lockUntil <= now);
    }

    private record Attempt(int failCount, long lockUntil) {
    }
}
