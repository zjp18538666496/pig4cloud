package com.pig4cloud.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试：轻量审批全流程——普通用户申请角色 → 超管通过 → 角色自动绑定（user_role落库）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApprovalFlowIT extends ItTestSupport {

    private static final String APPLICANT = "lili";      // 科技租户·产品经理（角色152）
    private static final int TARGET_ROLE_ID = 151;       // 科技租户·研发工程师（角色151）

    @Test
    @Order(1)
    @DisplayName("申请角色→审批通过→角色自动绑定")
    void approvalFlowEndToEnd() {
        // 1. 申请人（李丽）登录并提交角色申请
        HttpHeaders applicantHeaders = login(APPLICANT, "12345678");
        Map<String, Object> applyResult = post("/api/approval/apply",
                Map.of("applyType", "role_apply", "roleId", TARGET_ROLE_ID, "reason", "IT集成测试"), applicantHeaders);
        assertEquals(200, ((Number) applyResult.get("code")).intValue(),
                "申请应成功：" + applyResult.get("message"));

        // 2. 审批人（admin）在审批中心查到待审单并通过
        HttpHeaders adminHeaders = login("admin", "12345678");
        Map<String, Object> listResult = post("/api/approval/getLists",
                Map.of("status", "0", "applicant", APPLICANT, "page", 1L, "pageSize", 10), adminHeaders);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) ((Map<?, ?>) listResult.get("data")).get("rows");
        assertNotNull(rows, "应能查到待审单");
        Map<String, Object> target = rows.stream()
                .filter(r -> String.valueOf(r.get("title")).contains("研发工程师"))
                .findFirst().orElseThrow(() -> new AssertionError("未找到李丽的角色申请待审单"));
        Map<String, Object> approveResult = post("/api/approval/approve",
                Map.of("id", ((Number) target.get("id")).intValue(), "pass", true, "comment", "IT测试通过"), adminHeaders);
        assertEquals(200, ((Number) approveResult.get("code")).intValue(), "审批应成功");

        // 3. 落库验证：user_role已绑定 + 申请单状态已更新
        Integer userId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Integer.class, APPLICANT);
        Integer bound = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_role WHERE user_id = ? AND role_id = ?",
                Integer.class, userId, TARGET_ROLE_ID);
        assertEquals(1, bound, "角色应已自动绑定");
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM sys_approval WHERE id = ?", String.class, ((Number) target.get("id")).intValue());
        assertEquals("1", status, "申请单应为已通过");

        // 4. 重复绑定防护：同一申请重复通过应报“已审批完成”
        Map<String, Object> reApprove = post("/api/approval/approve",
                Map.of("id", ((Number) target.get("id")).intValue(), "pass", true), adminHeaders);
        assertTrue(((Number) reApprove.get("code")).intValue() != 200, "重复审批应被拒绝");
    }

    @Test
    @Order(2)
    @DisplayName("申请人重复提交同类待审单被去重拦截")
    void duplicateApplyRejected() throws Exception {
        HttpHeaders headers = login(APPLICANT, "12345678");
        // 第一次申请（若上一用例已绑定151则换一个未拥有的角色，如152产品经理）
        Map<String, Object> options = get("/api/approval/roleOptions", headers);
        assertEquals(200, ((Number) options.get("code")).intValue(),
                "roleOptions应成功: " + options.get("message"));
        List<Map<String, Object>> roleOptions = (List<Map<String, Object>>) options.get("data");
        Map<String, Object> anyRole = roleOptions.stream().findFirst()
                .orElse(null);
        if (anyRole == null) {
            return; // 没有可申请角色则跳过
        }
        // 错开@Idempotent窗口（3秒，留一倍余量），确保第二次申请命中的是业务去重而非幂等拦截
        Thread.sleep(6000);
        Map<String, Object> first = post("/api/approval/apply",
                Map.of("applyType", "role_apply", "roleId", anyRole.get("id"), "reason", "去重测试"), headers);
        assertEquals(200, ((Number) first.get("code")).intValue(), "首次申请应成功: " + first.get("message"));
        Thread.sleep(3100);
        Map<String, Object> second = post("/api/approval/apply",
                Map.of("applyType", "role_apply", "roleId", anyRole.get("id"), "reason", "去重测试"), headers);
        assertTrue(((Number) second.get("code")).intValue() != 200, "重复申请应被去重拦截");
    }
}
