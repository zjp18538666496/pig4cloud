package com.pig4cloud.auth.service;

import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.auth.online.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 内存存储定期清理：黑名单/验证码/失败锁定/找回密码验证码中的过期条目与失效在线会话
 */
@Component
@RequiredArgsConstructor
public class MemoryStoreCleaner {

    private final TokenBlacklist tokenBlacklist;
    private final OnlineUserStore onlineUserStore;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetService passwordResetService;

    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void clean() {
        tokenBlacklist.cleanExpired();
        onlineUserStore.cleanExpired();
        captchaService.cleanExpired();
        loginAttemptService.cleanExpired();
        passwordResetService.cleanExpired();
    }
}
