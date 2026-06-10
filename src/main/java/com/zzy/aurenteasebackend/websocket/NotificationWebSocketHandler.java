package com.zzy.aurenteasebackend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    private final ScheduledExecutorService scheduler= Executors.newScheduledThreadPool(1);
    private final ObjectMapper objectMapper=new ObjectMapper();



    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("🔌 新租客成功建立 WebSocket 全双工管道！当前在线会话数: {}", sessions.size());
        // 🚀 实战：一旦连接成功，立刻启动极速生产者，疯狂灌入流量，逼近崩溃临界点
//        triggerHighVelocityLoadSimulation(session);
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

    private void triggerHighVelocityLoadSimulation(WebSocketSession session) {
        scheduler.scheduleAtFixedRate(() -> {
            if (!session.isOpen()) return;
            try {
                // 模拟每秒发送 100 条海量降价通知（生产速率 100m/s，远远超出前端单线程渲染处理极限）
                for (int i = 0; i < 50; i++) {
                    Map<String, Object> mockNotice = Map.of(
                            "propertyId", 100L + ThreadLocalRandom.current().nextLong(50),
                            "title", "🚨 🔥 [Hurry Up] Huge Price Drop in Brisbane CBD Apartment " + UUID.randomUUID().toString().substring(0, 5),
                            "suburb", "Brisbane City",
                            "oldPrice", new BigDecimal("850.00"),
                            "newPrice", new BigDecimal("520.00")
                    );

                    String json = objectMapper.writeValueAsString(mockNotice);
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                System.err.println("发送失败: " + e.getMessage());
            }
        }, 0, 500, TimeUnit.MILLISECONDS); // 每 500毫秒疯狂高频倾倒一次
    }


}
