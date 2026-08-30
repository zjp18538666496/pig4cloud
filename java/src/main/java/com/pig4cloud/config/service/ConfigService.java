package com.pig4cloud.config.service;

import com.pig4cloud.config.entity.SysConfigEntity;
import com.pig4cloud.config.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统参数读取服务：全部配置缓存在内存，更新时失效重载。
 * 密码策略/登录锁定阈值/日志保留天数等运行参数统一从这里读取，改配置即时生效、无需重启
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper configMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    public String getValue(String key, String defaultValue) {
        ensureLoaded();
        return cache.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = getValue(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("配置[{}]不是合法整数: {}，使用默认值{}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBool(String key, boolean defaultValue) {
        String value = getValue(key, null);
        return value == null || value.isBlank() ? defaultValue : "true".equalsIgnoreCase(value.trim());
    }

    /**
     * 失效缓存（配置更新后调用，下次读取自动重载）
     */
    public void refresh() {
        synchronized (this) {
            loaded = false;
            cache.clear();
        }
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (this) {
            if (loaded) {
                return;
            }
            for (SysConfigEntity config : configMapper.selectList(null)) {
                cache.put(config.getConfig_key(), config.getConfig_value());
            }
            loaded = true;
        }
    }
}
