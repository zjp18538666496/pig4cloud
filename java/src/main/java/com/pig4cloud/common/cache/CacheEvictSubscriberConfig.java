package com.pig4cloud.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 缓存失效跨实例订阅（redis模式）：订阅pigx:cache-evict频道，
 * 收到广播后清空本机的Caffeine业务缓存（菜单树/租户品牌），实现多实例严格一致。
 * 与WS推送订阅（WsRedisSubscriberConfig）同模式；发布者自身也会收到，重复失效无害
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.store.type", havingValue = "redis")
@RequiredArgsConstructor
public class CacheEvictSubscriberConfig {

    private final BizCacheService bizCacheService;

    @Bean
    public RedisMessageListenerContainer cacheEvictListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                bizCacheService.evictAllLocal();
                log.debug("收到缓存失效广播，本机Caffeine已清空");
            } catch (Exception ex) {
                log.warn("缓存失效广播处理失败: {}", ex.getMessage());
            }
        }, new ChannelTopic(BizCacheService.CHANNEL));
        log.info("缓存失效跨实例广播已启用（Redis频道{}）", BizCacheService.CHANNEL);
        return container;
    }
}
