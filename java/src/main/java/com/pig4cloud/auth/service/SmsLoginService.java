package com.pig4cloud.auth.service;

import com.pig4cloud.common.store.StateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码登录（app.sms.enabled=true开启）：验证码5分钟有效、一次性。
 * app.sms.mock=true时验证码直接回传给前端，仅供开发联调（生产严禁开启）；
 * 真实发送需接入短信服务商SDK（阿里云/腾讯云），当前预留SendHook便于接入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsLoginService {

    private final StateStore stateStore;

    @Value("${app.sms.enabled:false}")
    private boolean enabled;

    @Value("${app.sms.mock:false}")
    private boolean mock;

    private final SecureRandom random = new SecureRandom();

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMock() {
        return mock;
    }

    /**
     * 发送验证码：返回mock模式的验证码（生产返回null）
     */
    public String sendCode(String mobile) {
        if (!enabled) {
            throw new BadCredentialsException("短信登录未开启");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stateStore.put("auth:sms:" + mobile, code, 5 * 60 * 1000L);
        if (mock) {
            log.warn("短信MOCK模式：手机号{}验证码{}直接返回前端（生产严禁开启mock）", mobile, code);
            return code;
        }
        // TODO: 接入短信服务商后在此调用真实发送，发送失败抛异常
        log.info("短信验证码已生成: {}（未接入服务商，仅记录日志）", mobile);
        return null;
    }

    /**
     * 校验验证码：错误/过期抛BadCredentialsException，成功后立即销毁（一次性）
     */
    public void verifyCode(String mobile, String code) {
        String cached = stateStore.get("auth:sms:" + mobile);
        if (cached == null) {
            throw new BadCredentialsException("短信验证码已过期，请重新获取");
        }
        stateStore.delete("auth:sms:" + mobile);
        if (!cached.equals(code == null ? "" : code.trim())) {
            throw new BadCredentialsException("短信验证码错误");
        }
    }
}
