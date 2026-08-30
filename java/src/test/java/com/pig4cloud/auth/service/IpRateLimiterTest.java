package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * IP频控单测：窗口计数超限拒绝
 */
public class IpRateLimiterTest {

    private StateStore stateStore;
    private IpRateLimiter limiter;

    @BeforeEach
    public void setUp() {
        stateStore = Mockito.mock(StateStore.class);
        limiter = new IpRateLimiter(stateStore);
    }

    @Test
    public void testOverLimitThrows() {
        when(stateStore.increment(anyString(), anyLong())).thenReturn(31L);
        Assertions.assertThrows(BizException.class,
                () -> limiter.checkLimit("login:ip", "1.2.3.4", 30, 600000L, "过于频繁"));
    }

    @Test
    public void testWithinLimitPasses() {
        when(stateStore.increment(anyString(), anyLong())).thenReturn(3L);
        limiter.checkLimit("login:ip", "1.2.3.4", 30, 600000L, "过于频繁");
    }

    @Test
    public void testBlankIdentitySkipped() {
        limiter.checkLimit("login:ip", "", 1, 1000L, "过于频繁");
        Mockito.verify(stateStore, Mockito.never()).increment(anyString(), anyLong());
    }

    /**
     * 内存StateStore行为验证（过期/前缀扫描/自增）
     */
    @Test
    public void testMemoryStateStoreBehavior() {
        com.pig4cloud.common.store.MemoryStateStore store = new com.pig4cloud.common.store.MemoryStateStore();
        store.put("k1", "v1", 1000L);
        Assertions.assertEquals("v1", store.get("k1"));
        store.put("k2", "v2", 0L);
        store.put("k3", "v3", 50L);
        Set<String> keys = store.keys("k");
        Assertions.assertTrue(keys.contains("k1"));
        Assertions.assertTrue(keys.contains("k2"));
        Assertions.assertEquals(1L, store.increment("cnt", 1000L));
        Assertions.assertEquals(2L, store.increment("cnt", 1000L));
        store.delete("k1");
        Assertions.assertNull(store.get("k1"));
    }
}
