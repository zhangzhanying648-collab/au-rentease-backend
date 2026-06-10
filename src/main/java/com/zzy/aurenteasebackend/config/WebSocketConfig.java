package com.zzy.aurenteasebackend.config;

import com.zzy.aurenteasebackend.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private NotificationWebSocketHandler notificationWebSocketHandler;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                // 🌟 升级：确保与你的 CorsConfig 保持高度一致
                .setAllowedOrigins("http://localhost:5173")
                .setAllowedOriginPatterns("*"); // 兜底允许模式匹配
    }
}
