package com.pig4cloud.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 业务热数据缓存（Caffeine本地缓存）：
 * - 菜单树：selectMenuLists按用户名+类型缓存，避免每次导航实时查库拼角色菜单
 * - 租户品牌：公开品牌查询与登录品牌下发缓存
 * 失效策略：TTL兜底（10分钟）+ 菜单/角色/用户角色绑定/租户套餐变更时主动evictAll
 * （与踢会话的敏感变更点一致）。多实例部署时本地缓存各自失效，TTL兜底保证最终一致，
 * 如需严格一致可换app.store.type=redis并扩写此类的evict为广播
 */
@Slf4j
@Component
public class BizCacheService {

    private final Cache<String, Object> menuCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(1000)
            .build();

    private final Cache<String, Object> brandCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(200)
            .build();

    /**
     * 读穿透获取
     */
    @SuppressWarnings("unchecked")
    public <T> T getMenuTree(String key, Supplier<T> loader) {
        return (T) menuCache.get(key, k -> loader.get());
    }

    public <T> T getBrand(String key, Supplier<T> loader) {
        return (T) brandCache.get(key, k -> loader.get());
    }

    public void evictMenus() {
        menuCache.invalidateAll();
        log.debug("菜单缓存已全量失效");
    }

    public void evictBrands() {
        brandCache.invalidateAll();
        log.debug("租户品牌缓存已全量失效");
    }

    /**
     * 全部业务缓存失效（缓存监控一键刷新用）
     */
    public void evictAll() {
        evictMenus();
        evictBrands();
    }

    /**
     * 业务缓存概况（缓存监控展示用）
     */
    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("menuCacheSize", menuCache.estimatedSize());
        data.put("brandCacheSize", brandCache.estimatedSize());
        return data;
    }
}
