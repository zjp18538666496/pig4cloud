package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import org.springframework.stereotype.Component;

/**
 * IP频控：StateStore计数+窗口TTL实现滑动窗口限流。
 * 登录接口按IP限尝试次数（防换用户名爆破），验证码接口按IP限获取频率
 */
@Component
public class IpRateLimiter {

    private final StateStore stateStore;

    public IpRateLimiter(StateStore stateStore) {
        this.stateStore = stateStore;
    }

    /**
     * 窗口内计数超限则抛业务异常
     *
     * @param bucket       业务桶名（如login:ip）
     * @param identity     限流维度标识（如客户端IP）
     * @param max          窗口内最大次数
     * @param windowMillis 窗口时长
     * @param message      超限提示
     */
    public void checkLimit(String bucket, String identity, int max, long windowMillis, String message) {
        if (identity == null || identity.isBlank()) {
            return;
        }
        long count = stateStore.increment(bucket + ":" + identity, windowMillis);
        if (count > max) {
            throw new BizException(message);
        }
    }
}
