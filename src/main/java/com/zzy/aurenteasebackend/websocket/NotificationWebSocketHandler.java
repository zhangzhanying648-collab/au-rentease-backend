package com.zzy.aurenteasebackend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final JsonComponentModule jsonComponentModule;

    public NotificationWebSocketHandler(JsonComponentModule jsonComponentModule) {
        this.jsonComponentModule = jsonComponentModule;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("🔌 新租客成功建立 WebSocket 全双工管道！当前在线会话数: {}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("❌ 租客断开管道连接。当前在线会话数: {}", sessions.size());
    }

    public void broadcastNotification(String messageJson){
        log.info("📡 正在通过 WebSocket 向全网广播降价利好消息...");
        for (WebSocketSession session : sessions) {
            if(session.isOpen()){
                try{
                    session.sendMessage(new TextMessage(messageJson));
                }catch (Exception e){
                    log.error("推送单条 Session 消息失败", e);
                }
            }
        }

    }


}
