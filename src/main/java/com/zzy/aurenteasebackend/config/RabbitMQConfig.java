package com.zzy.aurenteasebackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String PROPERTY_EXCHANGE = "property.exchange";
    public static final String PROPERTY_CHANGE_QUEUE = "property.change.queue";
    public static final String PROPERTY_ROUTING_KEY = "property.changed";

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
    @Bean
    public Queue propertyChangeQueue() {
        return new Queue(PROPERTY_CHANGE_QUEUE, true); // 开启持久化
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
}
