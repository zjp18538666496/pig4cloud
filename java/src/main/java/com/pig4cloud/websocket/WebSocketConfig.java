package com.pig4cloud.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置：/ws/{accessToken}，token校验在PushWebSocketHandler建连时完成
 * （SecurityConfig已对/ws/**放行HTTP握手）
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PushWebSocketHandler pushWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pushWebSocketHandler, "/ws/*").setAllowedOriginPatterns("*");
    }
}
