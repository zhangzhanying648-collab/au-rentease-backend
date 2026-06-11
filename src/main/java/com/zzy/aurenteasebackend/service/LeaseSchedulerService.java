package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaseSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(LeaseSchedulerService.class);

    private final RabbitTemplate rabbitTemplate;

    /**
     * 🌟 企业级落地：每日凌晨 2 点执行（扫描还有 30 天到期的租约）
     * * Cron 表达式解析: "0 0 2 * * ?"
     * 从左到右依次代表：秒(0) 分(0) 时(2) 天(*) 月(*) 周(?)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scanExpiringLeaseDaily(){
        log.info("⏰ [定时任务启动] 开始执行每日租约到期自动扫描...");
        List<String> mockLeaseIds = List.of("LEASE_9901", "LEASE_9902", "LEASE_9903");

        log.info("📊 [定时任务] 本次扫描共发现 {} 笔即将到期的租约，准备下发提醒通知...", mockLeaseIds.size());

        // 2. 🚀【核心联动】不直接发邮件，而是转换为消息扔进 RabbitMQ
        for (String leaseId : mockLeaseIds) {
            String notificationPayload = String.format(
                    "{\"type\":\"LEASE_EXPIRING\", \"leaseId\":\"%s\", \"title\":\"您的租约即将到期，请及时确认是否续租！\", \"suburb\":\"Brisbane\"}",
                    leaseId
            );

            // 复用你之前配好、挂了多个消费者实例的交换机和队列
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PROPERTY_EXCHANGE,
                    RabbitMQConfig.PROPERTY_ROUTING_KEY,
                    notificationPayload
            );

            log.info("📤 [定时任务] 已成功将租约 {} 的催续租通知转化为 MQ 消息泄洪。", leaseId);
        }

        log.info("✅ [定时任务结束] 本轮到期扫描及消息分发完毕。");

    }

    /**
     * 🧪 专门用于本地开发测试的定时任务
     * 机制：固定频率执行（Fixed Rate），每隔 10 秒钟执行一次，方便我们在控制台肉眼观测
     */
//    @Scheduled(fixedRate = 10000) // 10000 毫秒 = 10 秒
//    public void localDevelopmentTestTask() {
//        log.info("🔄 [测试定时任务] 滴答！10秒轮巡：正在检测是否有未支付的看房订金（Inspection Deposit）超时...");
//
//        // 此处可写检查逻辑：如果订金创建超过30分钟未支付，自动将房源状态回滚为“可申请（Available）”
//    }


}
