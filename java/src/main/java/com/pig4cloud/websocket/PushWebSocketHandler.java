package com.pig4cloud.websocket;

import com.pig4cloud.auth.online.TokenBlacklist;
import com.pig4cloud.auth.JwtUtils;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时推送WebSocket处理器：连接路径 /ws/{accessToken}，握手时校验JWT并登记用户会话。
 * 同一用户多标签页多个连接都保留；站内信/公告扇出后按userId推送刷新未读数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    private final JwtUtils jwtUtils;
    private final TokenBlacklist tokenBlacklist;
    private final UserMapper userMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        session.getAttributes().put("userId", userId);
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket连接建立 userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> set = userSessions.get((Long) userId);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    userSessions.remove((Long) userId);
                }
            }
        }
    }

    /**
     * 向用户的所有在线连接推送文本消息（JSON），连接无效自动清理
     */
    public void push(Long userId, String payload) {
        Set<WebSocketSession> set = userSessions.get(userId);
        if (set == null) {
            return;
        }
        set.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : set) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (Exception ex) {
                log.warn("WebSocket推送失败 userId={}: {}", userId, ex.getMessage());
            }
        }
    }

    /**
     * 从连接路径解析并校验token，返回用户id；无效返回null
     */
    private Long resolveUserId(WebSocketSession session) {
        try {
            String path = session.getUri() == null ? "" : session.getUri().getPath();
            String token = path.substring(path.lastIndexOf('/') + 1);
            Claims claims = jwtUtils.parseJwt(token);
            if (tokenBlacklist.isRevoked(claims.getId())) {
                return null;
            }
            String username = claims.get("username", String.class);
            UserEntity user = userMapper.selectUserByUsername(username);
            return user == null ? null : user.getId().longValue();
        } catch (Exception ex) {
            log.debug("WebSocket握手token无效: {}", ex.getMessage());
            return null;
        }
    }
}
