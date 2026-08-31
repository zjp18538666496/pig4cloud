package com.pig4cloud.monitor.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口指标拦截器（内存统计，自启动起累计，重启归零）：按"方法+路由模式"聚合
 * 调用次数/总耗时/最大耗时/错误数，供监控中心页面展示
 */
@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = "monitor:start";
    private static final String API_ATTR = "monitor:api";

    private final Map<String, Metric> metrics = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (start == null) {
            return;
        }
        long cost = System.currentTimeMillis() - (Long) start;
        // 用最佳匹配路由模式聚合（如/api/user/{id}），无匹配则退回servletPath
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String api = request.getMethod() + " " + (pattern == null ? request.getServletPath() : pattern);
        Metric metric = metrics.computeIfAbsent(api, key -> new Metric());
        metric.count.incrementAndGet();
        metric.totalMs.addAndGet(cost);
        metric.maxMs.updateAndGet(prev -> Math.max(prev, cost));
        if (response.getStatus() >= 400) {
            metric.errors.incrementAndGet();
        }
    }

    /**
     * 按调用量倒序的接口指标列表
     */
    public List<Map<String, Object>> topApis(int limit) {
        return metrics.entrySet().stream()
                .sorted(Map.Entry.<String, Metric>comparingByValue(
                        Comparator.comparingLong((Metric m) -> m.count.get()).reversed()))
                .limit(limit)
                .map(entry -> {
                    Metric metric = entry.getValue();
                    long count = metric.count.get();
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("api", entry.getKey());
                    item.put("count", count);
                    item.put("avgMs", count == 0 ? 0 : metric.totalMs.get() / count);
                    item.put("maxMs", metric.maxMs.get());
                    item.put("errors", metric.errors.get());
                    return item;
                })
                .toList();
    }

    public long totalRequests() {
        return metrics.values().stream().mapToLong(metric -> metric.count.get()).sum();
    }

    public long totalErrors() {
        return metrics.values().stream().mapToLong(metric -> metric.errors.get()).sum();
    }

    private static class Metric {
        final AtomicLong count = new AtomicLong();
        final AtomicLong totalMs = new AtomicLong();
        final AtomicLong maxMs = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
    }
}
