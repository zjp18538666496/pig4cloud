package com.pig4cloud.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试：多租户数据隔离——租户管理员只能看到本租户用户，跨租户管理接口无权限
 */
class TenantIsolationIT extends ItTestSupport {

    @Test
    @DisplayName("科技租户管理员只看到本租户用户")
    void tenantAdminSeesOnlyOwnUsers() {
        HttpHeaders headers = login("techadmin", "12345678");
        Map<String, Object> result = post("/api/user/getUserList", Map.of("page", 1L, "pageSize", 50), headers);
        assertEquals(200, ((Number) result.get("code")).intValue());

        List<Map<String, Object>> rows = (List<Map<String, Object>>) ((Map<?, ?>) result.get("data")).get("rows");
        assertTrue(rows != null && !rows.isEmpty(), "应能查到本租户用户");
        for (Map<String, Object> row : rows) {
            Object tenantId = row.get("tenant_id");
            assertEquals(10, ((Number) tenantId).intValue(),
                    "科技租户管理员不应看到其他租户用户，发现tenant_id=" + tenantId);
        }
    }

    @Test
    @DisplayName("租户管理员无平台管理权限")
    void tenantAdminDeniedPlatformApis() {
        HttpHeaders headers = login("techadmin", "12345678");
        Map<String, Object> result = post("/api/tenant/getTenantLists",
                Map.of("page", 1L, "pageSize", 10), headers);
        assertTrue(((Number) result.get("code")).intValue() != 200, "租户管理员不应能查租户列表");
    }

    @Test
    @DisplayName("平台超管能看到全部租户用户")
    void superAdminSeesAll() {
        HttpHeaders headers = login("admin", "12345678");
        Map<String, Object> result = post("/api/user/getUserList", Map.of("page", 1L, "pageSize", 50), headers);
        assertEquals(200, ((Number) result.get("code")).intValue());
        long total = ((Number) ((Map<?, ?>) result.get("data")).get("total")).longValue();
        assertTrue(total >= 9, "超管应看到全部演示用户");
    }
}
