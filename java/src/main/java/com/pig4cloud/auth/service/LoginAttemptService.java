package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import com.pig4cloud.config.service.ConfigService;
import org.springframework.stereotype.Component;

/**
 * 登录失败锁定：同一用户名窗口内连续失败达到上限后临时锁定。
 * 阈值与锁定时长走sys_config（login.max-attempts/login.lock-minutes）；
 * 状态存StateStore（TTL到期自动解锁），重启不丢（redis模式跨实例共享）。
 */
@Component
public class LoginAttemptService {

    private static final String CNT_PREFIX = "auth:attempt:cnt:";
    private static final String LOCK_PREFIX = "auth:attempt:lock:";

    private final StateStore stateStore;
    private final ConfigService configService;

    public LoginAttemptService(StateStore stateStore, ConfigService configService) {
        this.stateStore = stateStore;
        this.configService = configService;
    }

    /**
     * 登录前检查：锁定中则直接拒绝
     */
    public void checkLocked(String username) {
        if (stateStore.exists(LOCK_PREFIX + username)) {
            long lockMinutes = configService.getInt("login.lock-minutes", 10);
            throw new BizException("登录失败次数过多，账号已锁定，请约" + lockMinutes + "分钟后重试");
        }
    }

    public void recordFailure(String username) {
        int maxAttempts = configService.getInt("login.max-attempts", 5);
        long lockMillis = configService.getInt("login.lock-minutes", 10) * 60 * 1000L;
        long count = stateStore.increment(CNT_PREFIX + username, lockMillis);
        if (count >= maxAttempts) {
            // 达到上限：锁定并清空计数，锁定期满自动解锁
            stateStore.put(LOCK_PREFIX + username, "1", lockMillis);
            stateStore.delete(CNT_PREFIX + username);
        }
    }

    public void recordSuccess(String username) {
        stateStore.delete(CNT_PREFIX + username);
        stateStore.delete(LOCK_PREFIX + username);
    }
}
