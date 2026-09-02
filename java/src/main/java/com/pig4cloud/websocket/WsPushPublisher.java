package com.pig4cloud.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * WS推送发布器：多实例适配。
 * memory模式直接本机推送；redis模式发布到Redis频道pigx:ws-push，
 * 所有实例（含本实例）的订阅器收到后推给各自本地的该用户连接
 */
@Component
@RequiredArgsConstructor
public class WsPushPublisher {

    public static final String CHANNEL = "pigx:ws-push";

    private final PushWebSocketHandler pushHandler;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.store.type:memory}")
    private String storeType;

    /**
     * 向用户推送一条JSON文本消息（跨实例）
     */
    public void publish(Long userId, String payload) {
        if ("redis".equals(storeType)) {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                redisTemplate.convertAndSend(CHANNEL, userId + "|" + payload);
                return;
            }
        }
        pushHandler.push(userId, payload);
    }
}
