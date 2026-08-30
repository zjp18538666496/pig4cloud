package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 找回密码验证码（内存实现，单机有效）：6位数字码，5分钟有效，
 * 同一邮箱60秒内只允许发送一次，校验即消费。
 */
@Component
public class PasswordResetService {

    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000L;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, ResetEntry> store = new ConcurrentHashMap<>();

    /**
     * 生成并发送验证码；带发送频控
     */
    public void sendCode(String email, MailService mailService) {
        String key = normalize(email);
        ResetEntry entry = store.get(key);
        if (entry != null && entry.nextSendAt() > System.currentTimeMillis()) {
            throw new BizException("发送过于频繁，请1分钟后再试");
        }
        String code = String.format("%06d", random.nextInt(1000000));
        store.put(key, new ResetEntry(code, System.currentTimeMillis() + CODE_EXPIRE_MILLIS,
                System.currentTimeMillis() + SEND_INTERVAL_MILLIS));
        mailService.sendResetCodeMail(email, code);
    }

    /**
     * 校验并消费验证码
     */
    public void verify(String email, String code) {
        String key = normalize(email);
        ResetEntry entry = store.get(key);
        if (entry == null || entry.expireAt() <= System.currentTimeMillis()) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!entry.code().equals(code == null ? "" : code.trim())) {
            throw new BizException("验证码不正确");
        }
        store.remove(key);
    }

    public void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt() <= now);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record ResetEntry(String code, long expireAt, long nextSendAt) {
    }
}
