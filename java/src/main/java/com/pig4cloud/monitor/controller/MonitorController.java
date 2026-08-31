package com.pig4cloud.monitor.controller;

import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.common.result.R;
import com.pig4cloud.monitor.interceptor.ApiMetricsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口监控（monitor:read权限）：JVM运行信息 + 接口调用统计（内存统计，重启归零）
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ApiMetricsInterceptor metricsInterceptor;
    private final OnlineUserStore onlineUserStore;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('monitor:read')")
    public R<Map<String, Object>> overview() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uptimeMs", System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime());
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("processors", runtime.availableProcessors());
        data.put("heapUsedMb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        data.put("heapMaxMb", runtime.maxMemory() / 1024 / 1024);
        data.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        data.put("onlineCount", onlineUserStore.count());
        data.put("totalRequests", metricsInterceptor.totalRequests());
        data.put("totalErrors", metricsInterceptor.totalErrors());
        data.put("apis", metricsInterceptor.topApis(50));
        return R.ok("请求成功", data);
    }
}
