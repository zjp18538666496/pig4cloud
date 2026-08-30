package com.pig4cloud.auth.online;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线会话存储（内存实现，单机有效）：登录时注册，登出/强退/改密时移除。
 * key为access token的jti；refresh换新access token时重挂会话，避免刷新后"掉线"。
 */
@Component
public class OnlineUserStore {

    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();

    public void register(SessionRecord record) {
        sessions.put(record.getTokenJti(), record);
    }

    public SessionRecord findByAccessJti(String jti) {
        return jti == null ? null : sessions.get(jti);
    }

    public SessionRecord findByRefreshJti(String refreshJti) {
        if (refreshJti == null) {
            return null;
        }
        return sessions.values().stream()
                .filter(session -> refreshJti.equals(session.getRefreshJti()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 刷新access token后重挂会话：key由旧jti换成新jti
     */
    public void rekeyAccess(String oldJti, SessionRecord updated) {
        sessions.remove(oldJti);
        sessions.put(updated.getTokenJti(), updated);
    }

    public void touchAccess(String jti) {
        SessionRecord session = sessions.get(jti);
        if (session != null) {
            session.setLastAccessTime(new java.util.Date());
        }
    }

    public SessionRecord remove(String accessJti) {
        return accessJti == null ? null : sessions.remove(accessJti);
    }

    public List<SessionRecord> removeByUsername(String username) {
        List<SessionRecord> removed = new ArrayList<>();
        sessions.values().removeIf(session -> {
            if (session.getUsername().equals(username)) {
                removed.add(session);
                return true;
            }
            return false;
        });
        return removed;
    }

    /**
     * 按登录时间倒序的全量会话
     */
    public List<SessionRecord> list() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(SessionRecord::getLoginTime).reversed())
                .toList();
    }

    public int count() {
        return sessions.size();
    }

    /**
     * 清理失效会话：refresh token已过期（access必然也已过期）
     */
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(session -> session.getRefreshExpireAt() <= now);
    }
}
