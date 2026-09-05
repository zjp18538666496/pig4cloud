package com.pig4cloud.monitor.controller;

import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.common.result.R;
import com.pig4cloud.common.store.StateStore;
import com.pig4cloud.monitor.interceptor.ApiMetricsInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口监控（monitor:read权限）：JVM运行信息 + 接口调用统计（内存统计）；
 * redis模式下多实例节点心跳聚合（clusterRequests/clusterErrors为全集群汇总）
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ApiMetricsInterceptor metricsInterceptor;
    private final OnlineUserStore onlineUserStore;
    private final StateStore stateStore;
    private final ObjectMapper objectMapper;
    private final com.pig4cloud.monitor.service.CacheMonitorService cacheMonitorService;

    @Value("${app.store.type:memory}")
    private String storeType;

    /**
     * 缓存总览（仅平台超管：会暴露会话/验证码等敏感内容）
     */
    @GetMapping("/cache/overview")
    @PreAuthorize("hasAuthority('super')")
    public R<Map<String, Object>> cacheOverview() {
        return R.ok(cacheMonitorService.overview());
    }

    @GetMapping("/cache/keys")
    @PreAuthorize("hasAuthority('super')")
    public R<Map<String, Object>> cacheKeys(@org.springframework.web.bind.annotation.RequestParam(required = false) String group,
                                            @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword,
                                            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
                                            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(cacheMonitorService.listKeys(group, keyword,
                Math.max(1, page), Math.min(100, Math.max(1, pageSize))));
    }

    @org.springframework.web.bind.annotation.GetMapping("/cache/biz")
    @PreAuthorize("hasAuthority('super')")
    public R<Map<String, Object>> cacheBiz() {
        return R.ok(cacheMonitorService.bizStats());
    }

    @org.springframework.web.bind.annotation.PostMapping("/cache/clearBiz")
    @PreAuthorize("hasAuthority('super')")
    public R<Void> cacheClearBiz() {
        cacheMonitorService.clearBiz();
        return R.ok("业务本地缓存已刷新（菜单树/租户品牌将重新加载）", null);
    }

    @org.springframework.web.bind.annotation.PostMapping("/cache/clearGroup")
    @PreAuthorize("hasAuthority('super')")
    public R<Integer> cacheClearGroup(@org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {
        int deleted = cacheMonitorService.deleteGroup(body.get("group"));
        return R.ok("已删除" + deleted + "个键", deleted);
    }

    @org.springframework.web.bind.annotation.PostMapping("/cache/delete")
    @PreAuthorize("hasAuthority('super')")
    public R<Void> cacheDelete(@org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {
        cacheMonitorService.deleteKey(body.get("key"));
        return R.ok("删除成功", null);
    }

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

        // 多实例聚合：redis模式下汇总全部存活节点的心跳快照（本实例也在内）
        List<Map<String, Object>> nodes = new ArrayList<>();
        long clusterRequests = metricsInterceptor.totalRequests();
        long clusterErrors = metricsInterceptor.totalErrors();
        if ("redis".equals(storeType)) {
            for (String key : stateStore.keys("monitor:node:")) {
                try {
                    Map node = objectMapper.readValue(stateStore.get(key), Map.class);
                    nodes.add(node);
                    clusterRequests += ((Number) node.getOrDefault("totalRequests", 0)).longValue();
                    clusterErrors += ((Number) node.getOrDefault("totalErrors", 0)).longValue();
                } catch (Exception ignored) {
                    // 快照损坏直接跳过
                }
            }
        }
        data.put("nodeCount", Math.max(nodes.size(), 1));
        data.put("nodes", nodes);
        data.put("clusterRequests", clusterRequests);
        data.put("clusterErrors", clusterErrors);
        return R.ok("请求成功", data);
    }
}
