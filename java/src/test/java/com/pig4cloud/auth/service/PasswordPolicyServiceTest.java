package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.config.service.ConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 密码策略单测：最小长度/复杂度开关
 */
public class PasswordPolicyServiceTest {

    private ConfigService configService;
    private PasswordPolicyService policyService;

    @BeforeEach
    public void setUp() {
        configService = Mockito.mock(ConfigService.class);
        policyService = new PasswordPolicyService(configService);
    }

    @Test
    public void testMinLengthEnforced() {
        when(configService.getInt(eq("pwd.min-length"), Mockito.anyInt())).thenReturn(8);
        Assertions.assertThrows(BizException.class, () -> policyService.validate("Ab1"));
        policyService.validate("Ab123456");
    }

    @Test
    public void testComplexMode() {
        when(configService.getInt(eq("pwd.min-length"), Mockito.anyInt())).thenReturn(8);
        when(configService.getBool(eq("pwd.require-complex"), Mockito.anyBoolean())).thenReturn(true);
        // 纯数字不满足复杂度
        Assertions.assertThrows(BizException.class, () -> policyService.validate("12345678"));
        // 字母+数字通过
        policyService.validate("abcd1234");
    }

    @Test
    public void testTooLongRejected() {
        when(configService.getInt(eq("pwd.min-length"), Mockito.anyInt())).thenReturn(8);
        Assertions.assertThrows(BizException.class, () -> policyService.validate("a".repeat(65)));
    }
}
