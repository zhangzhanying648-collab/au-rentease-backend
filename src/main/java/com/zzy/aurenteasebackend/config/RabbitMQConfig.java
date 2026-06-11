package com.zzy.aurenteasebackend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    public static final String PROPERTY_EXCHANGE = "property.exchange";
    public static final String PROPERTY_CHANGE_QUEUE = "property.change.queue";
    public static final String PROPERTY_ROUTING_KEY = "property.changed";

    // 🌟 新增：死信组件的名字定义
    public static final String PROPERTY_DLQ_EXCHANGE = "property.dlq.exchange";
    public static final String PROPERTY_DLQ_QUEUE = "property.dlq.queue";
    public static final String PROPERTY_DLQ_ROUTING_KEY = "property.dlq.dead";


    // 报警专用的队列与路由键定义
    public static final String ALARM_QUEUE = "property.alarm.queue";
    public static final String ALARM_ROUTING_KEY = "property.alarm.triggered";



    /**
     * @Bean：告诉 Spring：“请执行这个方法，并把返回的 TopicExchange
     * 对象注册到 Spring 容器中管理”。Spring AMQP 探测到这个 Bean 后，会自动去 RabbitMQ 真正创建一个交换机。
     * TopicExchange：声明这是一个 通配符模式（Topic）交换机。这是大厂最常用的交换机类型，
     * 支持类似 property.# 或 property.* 的模糊匹配投递，扩展性极强。
     * @return
     */
    @Bean
    public TopicExchange propertyExchange() {
        return new TopicExchange(PROPERTY_EXCHANGE);
    }

    /**
     * 让 Spring 自动去 RabbitMQ 创建一个真实的队列。
     * true（核心大厂规范）：代表 durable（持久化）。
     * 意思是哪怕 RabbitMQ 服务器突然断电重启，这个队列以及队列里还没被消费的消息也不会丢失，数据安全拉满。
     * @return
     */
//    @Bean
//    public Queue propertyChangeQueue() {
//        Map<String, Object> args = new HashMap<>();
//        // 绑定死信交换机
//        args.put("x-dead-letter-exchange", PROPERTY_DLQ_EXCHANGE);
//        // 绑定死信路由键
//        args.put("x-dead-letter-routing-key", PROPERTY_DLQ_ROUTING_KEY);
//
//        return new Queue(PROPERTY_CHANGE_QUEUE, true, false, false, args);
//
////        return new Queue(PROPERTY_CHANGE_QUEUE, true); // 开启持久化
//    }
    @Bean
    public Queue propertyChangeQueue() {
        return QueueBuilder.durable(PROPERTY_CHANGE_QUEUE)
                // 🔒 优雅绑定死信交换机 (DLX)
                .deadLetterExchange(PROPERTY_DLQ_EXCHANGE)
                // 🔒 优雅绑定死信路由键 (DLK)
                .deadLetterRoutingKey(PROPERTY_DLQ_ROUTING_KEY)
                //将此队列设定为有界队列，最大消息条数限制为 10000 条
                .maxLength(10000)//
                 //【可选大闸】：限制这个队列总物理大小最大为 1GMB，双重防爆
//                .maxLengthBytes(1024 * 1024 * 1024)
                // 🌟【丢弃策略设置】：当满载时，消息的处理态度（默认采取 drop-head 策略）
//                .overflow(QueueBuilder.Overflow.rejectPublish)
                .build();
    }

    /**
     * 参数中的 Queue 和 TopicExchange：Spring 非常智能，当它看到这个方法的入参时，
     * 会自动去容器里把上面刚创建好的 propertyChangeQueue 和 propertyExchange 注入进来。
     *
     * BindingBuilder.bind(...).to(...).with(...)：这是 Spring 提供的流式 API（Fluent API），
     * 像写英文句子一样流畅。整行的意思是：
     *
     * “将 propertyChangeQueue（队列）绑定到 propertyExchange（交换机）上，并且规定：只有当消息带着
     * property.changed（路由键）过来时，才允许进入该队列。”
     * @param propertyChangeQueue
     * @param propertyExchange
     * @return
     */
    @Bean
    public Binding bindingPropertyChange(Queue propertyChangeQueue, TopicExchange propertyExchange) {
        return BindingBuilder.bind(propertyChangeQueue).to(propertyExchange).with(PROPERTY_ROUTING_KEY);
    }

    // ================= 🌟 以下为纯新增的死信“ICU”组件 =================

    @Bean
    public TopicExchange propertyDlqExchange() {
        return new TopicExchange(PROPERTY_DLQ_EXCHANGE);
    }

    @Bean
    public Queue propertyDlqQueue() {
        return new Queue(PROPERTY_DLQ_QUEUE, true); // 死信队列同样要持久化
    }

    @Bean
    public Binding bindingDlqQueue(Queue propertyDlqQueue, TopicExchange propertyDlqExchange) {
        return BindingBuilder.bind(propertyDlqQueue).to(propertyDlqExchange).with(PROPERTY_DLQ_ROUTING_KEY);
    }

    // 🌟【核心新增：工业级背压监听器工厂】
    @Bean(name = "backpressureContainerFactory")
    public SimpleRabbitListenerContainerFactory backpressureContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // 🛡️ 动作一：开启显式手动确认模式（Manual ACK）
        // 只有等我们消费完、群发完邮件，并手动执行 basicAck 后，MQ 才算这条消息消费成功
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 🛡️ 动作二：核心限流大闸 Prefetch = 2
        // 限制 RabbitMQ 每次只允许向当前消费者实例推送 2 条未确认的消息。
        // 当有 2 条邮件正在发送时，消费者的缓冲区满了，MQ 将处于制动（背压）状态，停止推送，
        // 消息稳稳地积压在 MQ 磁盘/队列中，保护微服务内存。
        factory.setPrefetchCount(2);

        // 🛡️ 动作三：根据业务需要，还可以调整消费者线程并发数（这里维持单节点或双并发）
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);

        return factory;
    }

    @Bean
    public Queue alarmQueue() {
        return new Queue(ALARM_QUEUE, true); // 独立的报警队列
    }

    @Bean
    public Binding bindingAlarmQueue(Queue alarmQueue, TopicExchange propertyExchange) {
        // 同样绑定到主交换机，但使用不同的路由键
        return BindingBuilder.bind(alarmQueue).to(propertyExchange).with(ALARM_ROUTING_KEY);
    }
}
