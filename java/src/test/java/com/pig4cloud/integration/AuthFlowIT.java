package com.pig4cloud.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试：登录链路（成功/错误密码/错误验证码）
 */
class AuthFlowIT extends ItTestSupport {

    @Test
    @DisplayName("正确密码+验证码登录成功并下发token")
    void loginSuccess() {
        HttpHeaders headers = login("admin", "12345678");
        assertTrue(headers.getFirst("Authorization").startsWith("Bearer "));
    }

    @Test
    @DisplayName("错误密码登录被拒绝")
    void loginWrongPassword() {
        Map<String, Object> captchaResponse = restTemplate
                .getForEntity(baseUrl() + "/api/auth/captcha", Map.class).getBody();
        Map<?, ?> data = (Map<?, ?>) captchaResponse.get("data");
        String captchaId = String.valueOf(data.get("captchaId"));
        String saved = stateStore.get("auth:captcha:" + captchaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "username", "admin",
                "password", "wrong-password",
                "captchaId", captchaId,
                "captchaCode", saved == null ? "" : saved);
        Map<String, Object> result = post("/api/auth/login", body, headers);
        assertEquals(-200, ((Number) result.get("code")).intValue());
    }

    @Test
    @DisplayName("未携带token访问业务接口被拒绝")
    void businessApiRequiresToken() {
        Map<String, Object> result = get("/api/monitor/overview", null);
        assertEquals(401, ((Number) result.get("code")).intValue());
    }

    @Test
    @DisplayName("已认证可访问健康自检")
    void healthEndpointWithAuth() {
        HttpHeaders headers = login("admin", "12345678");
        Map<String, Object> result = get("/api/health/detail", headers);
        assertEquals(200, ((Number) result.get("code")).intValue());
    }
}
