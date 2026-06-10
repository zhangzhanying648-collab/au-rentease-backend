package com.zzy.aurenteasebackend.consumer;

import com.zzy.aurenteasebackend.config.RabbitMQConfig;
import com.zzy.aurenteasebackend.controller.PropertyController;
import com.zzy.aurenteasebackend.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import com.rabbitmq.client.Channel;

@Component
@RequiredArgsConstructor
public class PropertyNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(PropertyNotificationConsumer.class);

    private final NotificationWebSocketHandler webSocketHandler;

    @RabbitListener(
            queues = RabbitMQConfig.PROPERTY_CHANGE_QUEUE,
            containerFactory = "backpressureContainerFactory"
    )
    public void handlePropertyPriceDrop(String messageJson,
                                        Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("[MQ Consumer {}] ⚡ 捕获到降价事件，准备推送前端: {}" ,Thread.currentThread().getName(), messageJson);



        //群发邮件
        try {
            // 🌟 核心测试埋点：如果消息体里包含 "BUG" 字样，立刻人为制造异常！
            if (messageJson.contains("BUG")) {
                log.warn("🚨 [测试埋点] 检测到模拟恶意毒丸消息，准备触发死信流转...");
                throw new RuntimeException("Simulated Mail Server Connection Timeout! (模拟邮件网关断网异常)");
            }

            // 🚀直接将 RabbitMQ 送出的 JSON 格式异步平推给全量在线 WebSocket 客户端
            webSocketHandler.broadcastNotification(messageJson);

            // 模拟调用第三方邮件服务（如 SendGrid）或短信关口发送大量通知
//            log.info("⏳ 正在为该区域的 5000+ 位意向租客批量生成个性化 EDM 营销邮件...");
            Thread.sleep(1500);
//            log.info("✔ [Email Service] 邮件全部异步派发完毕！中介端无任何卡顿。");

            // 🌟 3. 核心背压回执：显式手动确认（ACK）
            // 只有执行了这一步，RabbitMQ 的计数器才会减 1，才会放行下一条消息给这个线程
            // false 代表只确认当前这一条消息，不进行批量确认
            channel.basicAck(deliveryTag, false);
//            channel.basicReject(deliveryTag, false);
            log.info("[MQ Consumer{}] 消息 # {} 处理完毕，已成功向 MQ 发送 ACK 回执。", Thread.currentThread().getName(),deliveryTag);
        } catch (Exception e) {
            log.error("❌ [MQ Consumer] 处理过程中发生严重崩溃：", e);

            // 🛡️ 容错退路策略：
            // true 代表将消息重新放回队列头部（Requeue），让它重新排队或交由其他节点重试。
            // 如果连续失败，可以改为 false 并投递到死信队列（DLQ），防止死循环打爆系统。
//            channel.basicNack(deliveryTag, false, true);

            //核心修改：将第三个参数 requeue 设为 false！
            //告诉 RabbitMQ：“这条消息我搞不定，而且不要把它放回主队列头部了！”
            //此时，因为我们在 Config 里为主队列配了死信指向，RabbitMQ 会把这条消息【自动弹射】到死信队列中。
            channel.basicNack(deliveryTag, false, false);
            log.warn("⚠️ 毒丸消息已成功隔离，自动移送至死信队列（DLQ）。主干道恢复畅通！");
        }
    }

}
