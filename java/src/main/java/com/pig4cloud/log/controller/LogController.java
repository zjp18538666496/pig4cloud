package com.pig4cloud.log.controller;

import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.dto.LogQueryDto;
import com.pig4cloud.log.dto.LoginLogQueryDto;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.log.entity.OperateLog;
import com.pig4cloud.log.service.LoginLogService;
import com.pig4cloud.log.service.OperateLogService;
import lombok.RequiredArgsConstructor;
import com.pig4cloud.log.service.RollbackService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final OperateLogService operateLogService;
    private final RollbackService rollbackService;
    private final LoginLogService loginLogService;

    @PostMapping("/getOperateLogs")
    @PreAuthorize("hasAuthority('log:read')")
    public R<PageResult<OperateLog>> getOperateLogs(@RequestBody LogQueryDto dto) {
        // 租户隔离：非超管强制只查本租户日志；超管可按dto.tenantId筛选（空=全部）
        Integer tenantId = UserContext.isSuperTenant() ? dto.getTenantId() : UserContext.getTenantId();
        return R.ok("获取数据成功", operateLogService.pageQuery(dto.getUsername(), tenantId, dto.getPage(), dto.getPageSize()));
    }

    @PostMapping("/getLoginLogs")
    @PreAuthorize("hasAuthority('log:read')")
    public R<PageResult<LoginLog>> getLoginLogs(@RequestBody LoginLogQueryDto dto) {
        Integer tenantId = UserContext.isSuperTenant() ? dto.getTenantId() : UserContext.getTenantId();
        return R.ok("获取数据成功", loginLogService.pageQuery(dto.getUsername(), dto.getSuccess(), tenantId, dto.getPage(), dto.getPageSize()));
    }

    @PostMapping("/exportOperateLogs")
    @PreAuthorize("hasAuthority('log:read')")
    public void exportOperateLogs(@RequestBody LogQueryDto dto, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        Integer tenantId = UserContext.isSuperTenant() ? dto.getTenantId() : UserContext.getTenantId();
        var result = operateLogService.pageQuery(dto.getUsername(), tenantId, 1, 10000);
        var data = result.getRows().stream().map(log -> java.util.List.of(
                String.valueOf(log.getCreateTime() == null ? "" : log.getCreateTime()),
                nvl(log.getUsername()), nvl(log.getModule()), nvl(log.getOperation()),
                nvl(log.getUrl()), Boolean.TRUE.equals(log.getSuccess()) ? "成功" : "失败",
                nvl(log.getErrorMsg()))).toList();
        writeExcel(response, "操作日志",
                List.of("时间", "操作人", "模块", "操作", "接口地址", "结果", "失败信息"), data);
    }

    @PostMapping("/exportLoginLogs")
    @PreAuthorize("hasAuthority('log:read')")
    public void exportLoginLogs(@RequestBody LoginLogQueryDto dto, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        Integer tenantId = UserContext.isSuperTenant() ? dto.getTenantId() : UserContext.getTenantId();
        var result = loginLogService.pageQuery(dto.getUsername(), dto.getSuccess(), tenantId, 1, 10000);
        var data = result.getRows().stream().map(log -> java.util.List.of(
                String.valueOf(log.getCreateTime() == null ? "" : log.getCreateTime()),
                nvl(log.getUsername()), nvl(log.getIp()),
                Boolean.TRUE.equals(log.getSuccess()) ? "成功" : "失败", nvl(log.getMessage()))).toList();
        writeExcel(response, "登录日志",
                List.of("时间", "操作人", "来源IP", "结果", "说明"), data);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private void writeExcel(jakarta.servlet.http.HttpServletResponse response, String fileName,
                            List<String> headers, List<List<String>> data) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=utf-8''" + encoded + ".xlsx");
        com.alibaba.excel.EasyExcel.write(response.getOutputStream())
                .head(headers.stream().map(java.util.List::of).collect(java.util.stream.Collectors.toList()))
                .sheet("sheet1")
                .doWrite(data);
    }

    /**
     * 操作回滚（基于审计diff还原编辑前的值；角色/租户/用户管理的编辑操作）
     */
    @PostMapping("/rollback")
    @PreAuthorize("hasAuthority('log:read')")
    public R<String> rollback(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body) {
        return R.ok(rollbackService.rollback(body.get("logId")), null);
    }
}