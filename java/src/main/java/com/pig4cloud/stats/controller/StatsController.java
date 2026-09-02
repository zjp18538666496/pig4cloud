package com.pig4cloud.stats.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计：首页仪表盘（登录即可）+ 数据大屏租户报表（screen:read）
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok("请求成功", statsService.dashboard());
    }

    @GetMapping("/tenantReport")
    @PreAuthorize("hasAuthority('screen:read')")
    public R<List<Map<String, Object>>> tenantReport() {
        return R.ok("请求成功", statsService.tenantReport());
    }
}
