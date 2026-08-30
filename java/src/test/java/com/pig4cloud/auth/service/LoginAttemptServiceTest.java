package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import com.pig4cloud.config.service.ConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 登录失败锁定单测：计数达阈值即锁定，成功清除
 */
public class LoginAttemptServiceTest {

    private StateStore stateStore;
    private ConfigService configService;
    private LoginAttemptService service;

    @BeforeEach
    public void setUp() {
        stateStore = Mockito.mock(StateStore.class);
        configService = Mockito.mock(ConfigService.class);
        service = new LoginAttemptService(stateStore, configService);
        when(configService.getInt(eq("login.max-attempts"), Mockito.anyInt())).thenReturn(5);
        when(configService.getInt(eq("login.lock-minutes"), Mockito.anyInt())).thenReturn(10);
    }

    @Test
    public void testLockedWhenCounterReachesMax() {
        // 第5次失败达到阈值 → 写锁定键
        when(stateStore.increment(contains("cnt:"), anyLong())).thenReturn(5L);
        service.recordFailure("user1");
        Mockito.verify(stateStore).put(contains("lock:"), eq("1"), anyLong());
        // 锁定键存在时拒绝登录
        when(stateStore.exists(contains("lock:"))).thenReturn(true);
        Assertions.assertThrows(BizException.class, () -> service.checkLocked("user1"));
    }

    @Test
    public void testSuccessClearsAttempts() {
        service.recordSuccess("user1");
        Mockito.verify(stateStore).delete(contains("cnt:"));
        Mockito.verify(stateStore).delete(contains("lock:"));
    }

    @Test
    public void testNotLockedWhenBelowMax() {
        when(stateStore.increment(anyString(), anyLong())).thenReturn(2L);
        service.recordFailure("user1");
        Mockito.verify(stateStore, Mockito.never()).put(contains("lock:"), anyString(), anyLong());
    }
}
