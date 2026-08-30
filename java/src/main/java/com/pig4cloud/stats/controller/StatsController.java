package com.pig4cloud.stats.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页仪表盘统计（登录即可，数据范围随租户/数据权限自动隔离）
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
}
