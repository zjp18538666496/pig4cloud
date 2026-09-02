package com.pig4cloud.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * WS跨实例订阅配置（redis模式）：订阅pigx:ws-push频道，
 * 收到"userId|payload"后推给本实例上该用户的全部连接
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.store.type", havingValue = "redis")
@RequiredArgsConstructor
public class WsRedisSubscriberConfig {

    private final PushWebSocketHandler pushHandler;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer wsRedisListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                int split = body.indexOf('|');
                if (split <= 0) {
                    return;
                }
                Long userId = Long.valueOf(body.substring(0, split));
                String payload = body.substring(split + 1);
                pushHandler.push(userId, payload);
            } catch (Exception ex) {
                log.warn("WS跨实例消息处理失败: {}", ex.getMessage());
            }
        }, new ChannelTopic(WsPushPublisher.CHANNEL));
        log.info("WS跨实例推送已启用（Redis频道{}）", WsPushPublisher.CHANNEL);
        return container;
    }
}
