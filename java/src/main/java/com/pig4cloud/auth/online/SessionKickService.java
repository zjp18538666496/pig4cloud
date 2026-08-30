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
}
