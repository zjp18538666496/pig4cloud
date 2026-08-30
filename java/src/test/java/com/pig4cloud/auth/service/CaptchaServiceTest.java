package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 验证码服务单测：生成存储/一次性消费/不匹配拒绝
 */
public class CaptchaServiceTest {

    /**
     * 简易内存StateStore桩：记录put内容供断言
     */
    private StateStore spyStore(Map<String, String> backing) {
        StateStore store = Mockito.mock(StateStore.class);
        doAnswer(invocation -> {
            backing.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(store).put(anyString(), anyString(), anyLong());
        when(store.get(anyString())).thenAnswer(inv -> backing.get(inv.getArgument(0)));
        doAnswer(inv -> {
            backing.remove(inv.getArgument(0));
            return null;
        }).when(store).delete(anyString());
        when(store.exists(anyString())).thenAnswer(inv -> backing.containsKey(inv.getArgument(0)));
        return store;
    }

    @Test
    public void testVerifyConsumesCaptcha() {
        Map<String, String> backing = new ConcurrentHashMap<>();
        CaptchaService service = new CaptchaService(spyStore(backing));
        Map<String, String> captcha = service.generate();
        Assertions.assertNotNull(captcha.get("captchaId"));
        Assertions.assertTrue(captcha.get("image").startsWith("data:image/png;base64,"));
        // 找到生成的验证码答案（测试桩中仅有一条）
        String code = backing.values().iterator().next();
        service.verify(captcha.get("captchaId"), code.toLowerCase());
        // 二次使用：已消费 → 过期
        Assertions.assertThrows(BizException.class, () -> service.verify(captcha.get("captchaId"), code));
    }

    @Test
    public void testWrongCodeRejected() {
        Map<String, String> backing = new ConcurrentHashMap<>();
        CaptchaService service = new CaptchaService(spyStore(backing));
        Map<String, String> captcha = service.generate();
        Assertions.assertThrows(BizException.class, () -> service.verify(captcha.get("captchaId"), "ZZZZ"));
    }

    @Test
    public void testExpiredRejected() {
        CaptchaService service = new CaptchaService(Mockito.mock(StateStore.class));
        Assertions.assertThrows(BizException.class, () -> service.verify("not-exists", "AAAA"));
    }
}
