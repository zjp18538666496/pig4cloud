package com.pig4cloud.common.store;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis状态存储（app.store.type=redis时启用）：键TTL天然过期，支持多实例部署共享状态
 */
@Component
@ConditionalOnProperty(name = "app.store.type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisStateStore implements StateStore {

    private final StringRedisTemplate redisTemplate;

    private String k(String key) {
        return PREFIX + key;
    }

    @Override
    public void put(String key, String value, long ttlMillis) {
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(k(key), value, ttlMillis, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(k(key), value);
        }
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(k(key));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(k(key));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(k(key)));
    }

    @Override
    public long increment(String key, long ttlMillis) {
        Long value = redisTemplate.opsForValue().increment(k(key));
        // 仅首次（值为1）时设置TTL，避免每次自增都续期
        if (value != null && value == 1L && ttlMillis > 0) {
            redisTemplate.expire(key, ttlMillis, TimeUnit.MILLISECONDS);
        }
        return value == null ? 0L : value;
    }

    @Override
    public boolean putIfAbsent(String key, String value, long ttlMillis) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(k(key), value, ttlMillis, TimeUnit.MILLISECONDS));
    }

    @Override
    public Set<String> keys(String prefix) {
        Set<String> result = new LinkedHashSet<>();
        // SCAN增量遍历，避免KEYS阻塞Redis
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(k(prefix) + "*").count(500).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                result.add(key.startsWith(PREFIX) ? key.substring(PREFIX.length()) : key);
            }
        }
        return result;
    }
}
