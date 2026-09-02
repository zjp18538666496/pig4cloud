package com.pig4cloud.auth.online;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话强退服务：权限变更/禁用/删除等敏感操作后使相关用户在线会话立即失效
 * （access/refresh双token同时拉黑），避免权限残留到token自然过期
 */
@Service
@RequiredArgsConstructor
public class SessionKickService {

    private final OnlineUserStore onlineUserStore;
    private final TokenBlacklist tokenBlacklist;

    public void kickSession(SessionRecord session) {
        tokenBlacklist.revoke(session.getTokenJti(), session.getAccessExpireAt());
        tokenBlacklist.revoke(session.getRefreshJti(), session.getRefreshExpireAt());
        onlineUserStore.remove(session.getTokenJti());
    }

    public void kickUser(String username) {
        onlineUserStore.removeByUsername(username).forEach(this::kickSession);
    }

    public void kickUsernames(List<String> usernames) {
        if (usernames == null) {
            return;
        }
        usernames.forEach(this::kickUser);
    }

    public void kickTenant(Integer tenantId) {
        onlineUserStore.removeByTenant(tenantId).forEach(this::kickSession);
    }

    /**
     * 并发设备数限制：同账号会话超过max时从最旧开始下线（当前登录是最新一条，天然保留）
     */
    public void enforceSessionLimit(String username, int max) {
        List<SessionRecord> sessions = onlineUserStore.list().stream()
                .filter(session -> username.equals(session.getUsername()))
                // list()为最新在前，此处按最旧在前排序后逐个下线
                .sorted(java.util.Comparator.comparing(SessionRecord::getLoginTime))
                .toList();
        int excess = sessions.size() - max;
        for (int i = 0; i < excess; i++) {
            kickSession(sessions.get(i));
        }
    }
}
