package com.pig4cloud.monitor.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 监控节点心跳（redis模式）：每30秒把本实例的指标快照写入Redis（90秒TTL），
 * 多实例部署时overview接口汇总全部存活节点，实现跨实例请求数/错误数聚合
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.store.type", havingValue = "redis")
@RequiredArgsConstructor
public class MonitorNodeReporter {

    private final ApiMetricsInterceptor metricsInterceptor;
    private final com.pig4cloud.common.store.StateStore stateStore;
    private final ObjectMapper objectMapper;

    /**
     * 节点id（实例启动时生成）
     */
    private final String nodeId = UUID.randomUUID().toString().substring(0, 8);
    private final long startedAt = System.currentTimeMillis();

    @Scheduled(fixedDelay = 30 * 1000L, initialDelay = 20 * 1000L)
    public void report() {
        try {
            Map<String, Object> snapshot = Map.of(
                    "nodeId", nodeId,
                    "totalRequests", metricsInterceptor.totalRequests(),
                    "totalErrors", metricsInterceptor.totalErrors(),
                    "uptimeMs", System.currentTimeMillis() - startedAt,
                    "reportedAt", System.currentTimeMillis());
            // TTL 90秒 > 上报间隔30秒：节点宕机后快照自动过期，overview不再统计
            stateStore.put("monitor:node:" + nodeId, objectMapper.writeValueAsString(snapshot), 90 * 1000L);
        } catch (Exception ex) {
            log.warn("监控节点心跳上报失败: {}", ex.getMessage());
        }
    }
}
