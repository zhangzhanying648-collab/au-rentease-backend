package com.zzy.aurenteasebackend.consumer;

import com.zzy.aurenteasebackend.config.RabbitMQConfig;
import com.zzy.aurenteasebackend.controller.PropertyController;
import com.zzy.aurenteasebackend.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(PropertyNotificationConsumer.class);

    private final NotificationWebSocketHandler webSocketHandler;

    @RabbitListener(queues = RabbitMQConfig.PROPERTY_CHANGE_QUEUE)
    public void handlePropertyPriceDrop(String messageJson){
        log.info("[MQ Consumer] ⚡ 收到降价广播事件: " + messageJson);

        log.info("[MQ Consumer] ⚡ 捕获到降价事件，准备推送前端: " + messageJson);

        // 🚀直接将 RabbitMQ 送出的 JSON 格式异步平推给全量在线 WebSocket 客户端
        webSocketHandler.broadcastNotification(messageJson);

        //群发邮件
        try{
            // 模拟调用第三方邮件服务（如 SendGrid）或短信关口发送大量通知
            log.info("⏳ 正在为该区域的 5000+ 位意向租客批量生成个性化 EDM 营销邮件...");
            Thread.sleep(3000);
            log.info("✔ [Email Service] 邮件全部异步派发完毕！中介端无任何卡顿。");
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

}
