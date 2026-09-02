package com.pig4cloud.common.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存状态存储（默认实现，单机有效）：重启清空、多实例不共享，生产多实例部署请切app.store.type=redis
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.store.type", havingValue = "memory", matchIfMissing = true)
public class MemoryStateStore implements StateStore {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value, long ttlMillis) {
        long expireAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0L;
        store.put(key, new Entry(value, expireAt));
    }

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt > 0 && entry.expireAt <= System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public long increment(String key, long ttlMillis) {
        synchronized (this) {
            long value;
            Entry entry = store.get(key);
            if (entry != null && (entry.expireAt <= 0 || entry.expireAt > System.currentTimeMillis())) {
                value = Long.parseLong(entry.value) + 1;
            } else {
                value = 1;
            }
            put(key, Long.toString(value), ttlMillis);
            return value;
        }
    }

    @Override
    public boolean putIfAbsent(String key, String value, long ttlMillis) {
        synchronized (this) {
            if (exists(key)) {
                return false;
            }
            put(key, value, ttlMillis);
            return true;
        }
    }

    @Override
    public Set<String> keys(String prefix) {
        Set<String> result = new java.util.LinkedHashSet<>();
        store.forEach((key, entry) -> {
            if (key.startsWith(prefix) && (entry.expireAt <= 0 || entry.expireAt > System.currentTimeMillis())) {
                result.add(key);
            }
        });
        return result;
    }

    /**
     * 定期清理过期条目（仅memory模式注册）
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt > 0 && entry.getValue().expireAt <= now);
    }

    private record Entry(String value, long expireAt) {
    }
}
