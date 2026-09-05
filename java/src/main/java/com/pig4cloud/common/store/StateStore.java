package com.pig4cloud.common.store;

import java.util.Set;

/**
 * 状态存储抽象：验证码/失败锁定/找回码/token黑名单/在线会话/IP限流等一次性状态统一走此接口。
 * memory实现（默认，单机内存+定期清理）与redis实现（app.store.type=redis，键TTL天然过期，支持多实例）按配置切换。
 */
public interface StateStore {

    /**
     * 统一键前缀：与共用Redis的其它系统隔离（缓存监控/排查时一眼可辨）
     */
    String PREFIX = "pigx:";


    /**
     * 写入键值
     *
     * @param ttlMillis 过期毫秒数；&lt;=0表示不过期
     */
    void put(String key, String value, long ttlMillis);

    String get(String key);

    void delete(String key);

    boolean exists(String key);

    /**
     * 自增计数（键不存在从0开始），并保证TTL（仅首次写入时设置，避免续期）
     *
     * @return 自增后的值
     */
    long increment(String key, long ttlMillis);

    /**
     * 不存在时才写入（分布式锁原语）
     *
     * @return true=写入成功(获得锁)；false=键已存在
     */
    boolean putIfAbsent(String key, String value, long ttlMillis);

    /**
     * 按前缀取全部有效键
     */
    Set<String> keys(String prefix);
}
