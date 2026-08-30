package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 找回密码验证码：6位数字码存StateStore（5分钟TTL自动过期），
 * 同一邮箱60秒内只允许发送一次（冷却键TTL控制），校验即消费。
 */
@Component
public class PasswordResetService {

    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000L;
    private static final String CODE_PREFIX = "auth:resetcode:";
    private static final String COOL_PREFIX = "auth:resetcool:";

    private final SecureRandom random = new SecureRandom();
    private final StateStore stateStore;

    public PasswordResetService(StateStore stateStore) {
        this.stateStore = stateStore;
    }

    /**
     * 生成并发送验证码；带发送频控
     */
    public void sendCode(String email, MailService mailService) {
        String key = normalize(email);
        if (stateStore.exists(COOL_PREFIX + key)) {
            throw new BizException("发送过于频繁，请1分钟后再试");
        }
        String code = String.format("%06d", random.nextInt(1000000));
        stateStore.put(CODE_PREFIX + key, code, CODE_EXPIRE_MILLIS);
        stateStore.put(COOL_PREFIX + key, "1", SEND_INTERVAL_MILLIS);
        mailService.sendResetCodeMail(email, code);
    }

    /**
     * 校验并消费验证码
     */
    public void verify(String email, String code) {
        String key = normalize(email);
        String saved = stateStore.get(CODE_PREFIX + key);
        if (saved == null) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!saved.equals(code == null ? "" : code.trim())) {
            throw new BizException("验证码不正确");
        }
        stateStore.delete(CODE_PREFIX + key);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
