package com.pig4cloud.notify.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.common.mq.BufferedExecutor;
import com.pig4cloud.notify.entity.NotifyLog;
import com.pig4cloud.notify.entity.SysEventWebhookEntity;
import com.pig4cloud.notify.mapper.SysEventWebhookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * 事件出站推送：内部事件(公告发布/审批结果/任务失败/租户预警等)以签名Webhook推给外部系统。
 * 签名：X-Timestamp + X-Signature=HMAC-SHA256(secret, timestamp+body)，第三方验签防伪造。
 * 经BufferedExecutor削峰，失败重试最多3次，投递明细落MongoDB webhook_log
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPushService {

    private static final int MAX_ATTEMPTS = 3;

    private final SysEventWebhookMapper webhookMapper;
    private final BufferedExecutor bufferedExecutor;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 事件分发入口（异步、吞异常）：匹配订阅了该事件的启用Webhook逐个推送
     */
    public void dispatch(String eventCode, Map<String, Object> payload) {
        try {
            List<SysEventWebhookEntity> webhooks = webhookMapper.selectList(
                    new QueryWrapper<SysEventWebhookEntity>().eq("status", "1"));
            for (SysEventWebhookEntity webhook : webhooks) {
                String events = webhook.getEvents() == null ? "" : webhook.getEvents();
                boolean subscribed = List.of(events.split(",")).stream()
                        .map(String::trim).anyMatch(eventCode::equals);
                if (subscribed) {
                    bufferedExecutor.submit(() -> pushWithRetry(webhook, eventCode, payload),
                            "webhook:" + webhook.getId());
                }
            }
        } catch (Exception ex) {
            log.warn("事件[{}]出站分发失败: {}", eventCode, ex.getMessage());
        }
    }

    /**
     * 推送+失败重试（间隔1s/2s），每次尝试落投递记录
     */
    private void pushWithRetry(SysEventWebhookEntity webhook, String eventCode, Map<String, Object> payload) {
        String error = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long start = System.currentTimeMillis();
            error = attempt0(webhook, eventCode, payload);
            long cost = System.currentTimeMillis() - start;
            saveDeliveryLog(webhook, eventCode, error, attempt, cost);
            if (error == null) {
                return;
            }
            try {
                Thread.sleep(attempt * 1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Webhook[{}]事件{}推送最终失败: {}", webhook.getWebhook_name(), eventCode, error);
    }

    /**
     * 单次推送：POST JSON {event, data, timestamp}，带X-Timestamp/X-Signature头
     */
    String attempt0(SysEventWebhookEntity webhook, String eventCode, Map<String, Object> payload) {
        try {
            long timestamp = System.currentTimeMillis();
            String body = objectMapper.writeValueAsString(Map.of(
                    "event", eventCode,
                    "data", payload == null ? Map.of() : payload,
                    "timestamp", timestamp));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.getUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (webhook.getSecret() != null && !webhook.getSecret().isBlank()) {
                builder.header("X-Timestamp", String.valueOf(timestamp));
                builder.header("X-Signature", sign(webhook.getSecret(), timestamp, body));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return null;
            }
            return "HTTP " + response.statusCode();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private String sign(String secret, long timestamp, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal((timestamp + body).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("签名失败", ex);
        }
    }

    private void saveDeliveryLog(SysEventWebhookEntity webhook, String eventCode,
                                 String error, int attempt, long costMs) {
        try {
            NotifyLog logEntity = new NotifyLog();
            logEntity.setChannelId(webhook.getId());
            logEntity.setChannelName(webhook.getWebhook_name());
            logEntity.setChannelType("outbound");
            logEntity.setTemplateCode(eventCode);
            logEntity.setTitle("事件推送(" + eventCode + ")第" + attempt + "次");
            logEntity.setReceiver(webhook.getUrl());
            logEntity.setSuccess(error == null);
            logEntity.setMessage(error == null ? "OK"
                    : error.substring(0, Math.min(200, error.length())));
            logEntity.setCostMs(costMs);
            logEntity.setCreateTime(new Date());
            mongoTemplate.save(logEntity);
        } catch (Exception ex) {
            log.warn("Webhook投递记录写入失败: {}", ex.getMessage());
        }
    }
}
