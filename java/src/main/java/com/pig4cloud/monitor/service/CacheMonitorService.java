package com.pig4cloud.monitor.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 缓存监控（仅平台超管）：查看StateStore当前缓存（redis或内存模式）。
 * 分组统计 → 键明细（TTL/值预览）→ 删除；验证码/在线会话/token黑名单/任务锁等一次性状态都在这里
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheMonitorService {

    private static final int PREVIEW_LENGTH = 200;
    private static final int MAX_KEYS = 2000;

    private final StateStore stateStore;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final com.pig4cloud.common.cache.BizCacheService bizCacheService;

    @Value("${app.store.type:memory}")
    private String storeType;

    public boolean isRedis() {
        return "redis".equals(storeType);
    }

    /**
     * 总览：模式/键总数/分组统计/redis基础信息
     */
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", storeType);
        Set<String> allKeys = stateStore.keys("");
        data.put("totalKeys", allKeys.size());
        data.put("groups", groups());
        if (isRedis()) {
            StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
            if (redis != null) {
                try {
                    data.put("dbSize", redis.execute((org.springframework.data.redis.core.RedisCallback<Long>)
                            conn -> conn.serverCommands().dbSize()));
                    java.util.Properties info = redis.execute((org.springframework.data.redis.core.RedisCallback<java.util.Properties>)
                            conn -> conn.serverCommands().info("memory"));
                    if (info != null) {
                        data.put("usedMemoryHuman", info.getProperty("used_memory_human"));
                        data.put("usedMemoryPeakHuman", info.getProperty("used_memory_peak_human"));
                    }
                } catch (Exception ex) {
                    log.warn("Redis信息读取失败: {}", ex.getMessage());
                }
            }
        }
        return data;
    }

    /**
     * 按前缀分组统计（取键第一个':'之前的部分为组名），按数量降序
     */
    public List<Map<String, Object>> groups() {
        Map<String, Integer> counter = new TreeMap<>();
        for (String key : stateStore.keys("")) {
            int idx = key.indexOf(':');
            String group = idx > 0 ? key.substring(0, idx) : key;
            counter.merge(group, 1, Integer::sum);
        }
        return counter.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(50)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("group", e.getKey());
                    row.put("count", e.getValue());
                    return row;
                })
                .toList();
    }

    /**
     * 键明细：按组/关键字过滤，带TTL与值预览（截断200字符）
     */
    public Map<String, Object> listKeys(String group, String keyword, int page, int pageSize) {
        List<String> keys = stateStore.keys("").stream()
                .filter(k -> group == null || group.isBlank() || k.startsWith(group + ":") || k.equals(group))
                .filter(k -> keyword == null || keyword.isBlank() || k.contains(keyword))
                .sorted()
                .toList();
        long total = keys.size();
        int from = (int) Math.min(keys.size(), Math.max(0, (long) (page - 1) * pageSize));
        int to = (int) Math.min(keys.size(), (long) from + pageSize);
        StringRedisTemplate redis = isRedis() ? redisTemplateProvider.getIfAvailable() : null;
        List<Map<String, Object>> rows = keys.subList(from, to).stream().map(k -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", k);
            if (redis != null) {
                try {
                    Long ttl = redis.getExpire(k);
                    row.put("ttlSeconds", ttl == null ? null : ttl.intValue());
                } catch (Exception ex) {
                    row.put("ttlSeconds", null);
                }
            } else {
                row.put("ttlSeconds", null);
            }
            row.put("valuePreview", preview(k));
            return row;
        }).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("rows", rows);
        return data;
    }

    /**
     * 按前缀分组批量删除（危险操作）：删除该分组下全部键，返回删除数量
     */
    public int deleteGroup(String group) {
        if (group == null || group.isBlank() || group.contains("*")) {
            throw new BizException("非法的分组名");
        }
        Set<String> keys = stateStore.keys("");
        int deleted = 0;
        for (String key : keys) {
            if (key.equals(group) || key.startsWith(group + ":")) {
                stateStore.delete(key);
                deleted++;
            }
        }
        log.warn("缓存分组[{}]被平台超管批量删除{}个键", group, deleted);
        return deleted;
    }

    /**
     * 业务本地缓存（菜单/品牌Caffeine）概况
     */
    public Map<String, Object> bizStats() {
        return bizCacheService.stats();
    }

    /**
     * 业务本地缓存一键刷新（菜单/角色/套餐/租户变更后如未自动失效可手动刷新）
     */
    public void clearBiz() {
        bizCacheService.evictAll();
    }

    /**
     * 删除单个缓存键（危险操作：仅平台超管，删除后相关会话/验证码即失效）
     */
    public void deleteKey(String key) {
        if (key == null || key.isBlank() || key.contains("*")) {
            throw new BizException("非法的缓存键");
        }
        stateStore.delete(key);
        log.warn("缓存键[{}]被平台超管手动删除", key);
    }

    private String preview(String key) {
        try {
            String value = stateStore.get(key);
            if (value == null) {
                return "(不存在或非字符串类型)";
            }
            return value.length() > PREVIEW_LENGTH ? value.substring(0, PREVIEW_LENGTH) + "…" : value;
        } catch (Exception ex) {
            return "(读取失败)";
        }
    }
}
