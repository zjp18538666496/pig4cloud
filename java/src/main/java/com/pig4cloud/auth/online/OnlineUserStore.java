package com.pig4cloud.auth.online;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.common.store.StateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 在线会话存储：登录时注册，登出/强退/改密时移除。
 * 会话以access token的jti为主键，JSON序列化存StateStore（TTL=refresh token剩余有效期）；
 * refresh换新access token时重挂会话，避免刷新后"掉线"。
 * memory实现单机有效、redis实现支持多实例。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserStore {

    private static final String KEY_PREFIX = "online:session:";

    private final StateStore stateStore;
    private final ObjectMapper objectMapper;

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }

    public void register(SessionRecord record) {
        put(record);
    }

    private void put(SessionRecord record) {
        long ttl = record.getRefreshExpireAt() - System.currentTimeMillis();
        if (ttl <= 0) {
            return;
        }
        try {
            stateStore.put(key(record.getTokenJti()), objectMapper.writeValueAsString(record), ttl);
        } catch (JsonProcessingException ex) {
            log.error("在线会话序列化失败: {}", ex.getMessage());
        }
    }

    private SessionRecord deserialize(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SessionRecord.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    public SessionRecord findByAccessJti(String jti) {
        return jti == null ? null : deserialize(stateStore.get(key(jti)));
    }

    public SessionRecord findByRefreshJti(String refreshJti) {
        if (refreshJti == null) {
            return null;
        }
        return all().stream()
                .filter(session -> refreshJti.equals(session.getRefreshJti()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 刷新access token后重挂会话：key由旧jti换成新jti
     */
    public void rekeyAccess(String oldJti, SessionRecord updated) {
        stateStore.delete(key(oldJti));
        put(updated);
    }

    public void touchAccess(String jti) {
        SessionRecord session = findByAccessJti(jti);
        if (session != null) {
            session.setLastAccessTime(new java.util.Date());
            put(session);
        }
    }

    public SessionRecord remove(String accessJti) {
        SessionRecord session = findByAccessJti(accessJti);
        if (session != null) {
            stateStore.delete(key(accessJti));
        }
        return session;
    }

    public List<SessionRecord> removeByUsername(String username) {
        return removeMatch(session -> username.equals(session.getUsername()));
    }

    public List<SessionRecord> removeByTenant(Integer tenantId) {
        return removeMatch(session -> Objects.equals(tenantId, session.getTenantId()));
    }

    private List<SessionRecord> removeMatch(Predicate<SessionRecord> predicate) {
        List<SessionRecord> removed = new ArrayList<>();
        for (String k : stateStore.keys(KEY_PREFIX)) {
            SessionRecord session = deserialize(stateStore.get(k));
            if (session != null && predicate.test(session)) {
                removed.add(session);
                stateStore.delete(k);
            }
        }
        return removed;
    }

    private List<SessionRecord> all() {
        List<SessionRecord> sessions = new ArrayList<>();
        for (String k : stateStore.keys(KEY_PREFIX)) {
            SessionRecord session = deserialize(stateStore.get(k));
            if (session != null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    /**
     * 按登录时间倒序的全量会话
     */
    public List<SessionRecord> list() {
        return all().stream()
                .sorted(Comparator.comparing(SessionRecord::getLoginTime).reversed())
                .toList();
    }

    public int count() {
        return all().size();
    }
}
