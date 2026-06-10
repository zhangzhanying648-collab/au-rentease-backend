package com.zzy.aurenteasebackend.controller;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.zzy.aurenteasebackend.config.RabbitMQConfig;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.service.ApplicationService;
import com.zzy.aurenteasebackend.service.FileStorageService;
import com.zzy.aurenteasebackend.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {
    private static final Logger log = LoggerFactory.getLogger(PropertyController.class);

    private final PropertyService propertyService;
    private final PropertyRepository propertyRepository;
    private final FileStorageService fileStorageService;
    private final RabbitTemplate rabbitTemplate;


    // 🛡️ 权限大闸：系统执行这个方法前，会自动去检查当前用户的 authorities 集合里有没有 "ROLE_TENANT"
    // 如果没有（比如一个未登录用户或者房东撞进来了），直接拦截并返回 403 Forbidden（拒绝访问）！
//    @PreAuthorize("hasRole('TENANT')")
    // 💡 澳洲大厂规范：检索类接口一律用 GET 请求，通过 QueryString 传参

//    @GetMapping("/search")
    @RequestMapping(value = "/search", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<List<Property>> search(PropertySearchCriteria criteria) {
        List<Property> result = propertyService.searchProperties(criteria);
        for (int i = 0; i < result.size(); i++) {
            Property property = result.get(i);
            if(property.getImageUrl().startsWith("http:") || property.getImageUrl().startsWith("https:")){
                continue;
            }
            String objectKey = property.getImageUrl();
            if(objectKey!=null && !objectKey.isEmpty()) {
                String secureViewUrl = fileStorageService.generateViewUrl(objectKey);
                property.setImageUrl(secureViewUrl);
            }
        }
        return ResponseEntity.ok(result);
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<Property> getPropertyById(@PathVariable Long id) {
//        return propertyService.getPropertyById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }

    @GetMapping("/{id}")
    // 在获取单个房源详情的 Controller / Service 中：
    public ResponseEntity<Property> getPropertyById(@PathVariable Long id) {
        Property property = propertyRepository.findById(id).get();
        if(property == null){
            return ResponseEntity.notFound().build();
        }
        // 🌟 在这里！把数据库里的 Key 扔进你写好的签名官里，换取带有 1 小时时效、可被 <img> 直接渲染的绝对路径
        String secureViewUrl = fileStorageService.generateViewUrl(property.getImageUrl());
        property.setImageUrl(secureViewUrl); // 赋予 DTO

        return ResponseEntity.ok(property); // 这样前端详情页就能直接拿到完美的物理高时效链接啦！
    }

    // 🚀 响应前端 LandlordDashboard 中：await api.post('/properties', formData);
    @PostMapping("/create")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<Property> createProperty(@RequestBody Property dto) {
        Property saved = propertyService.saveOrUpdateProperty(null, dto);
        return ResponseEntity.ok(saved);
    }


    // 🚀 响应前端 LandlordDashboard 中：await api.put(`/properties/${editingPropertyId}`, payload);
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<Property> updateProperty(
            @PathVariable Long id,
            @RequestBody Property dto) {
        Property oldProperty = propertyRepository.findById(id).orElse(null);
        BigDecimal oldPrice=(oldProperty!=null)?oldProperty.getPricePerWeek():null;

        Property updated = propertyService.saveOrUpdateProperty(id, dto);

        //如果租金降价，则往MQ中发送消息
        if((oldPrice != null) && (updated.getPricePerWeek().compareTo(oldPrice)<0)){
            String messagePayload=String.format(
                    "{\"propertyId\":%d, \"title\":\"%s\", \"oldPrice\":%.2f, \"newPrice\":%.2f, \"suburb\":\"%s\"}",
                    updated.getId(), updated.getTitle(), oldPrice, updated.getPricePerWeek(), updated.getSuburb()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PROPERTY_EXCHANGE,
                    RabbitMQConfig.PROPERTY_ROUTING_KEY,
                    messagePayload
            );
            log.info("[MQ Producer] 📢 监测到资产降价，已成功向队列抛出通知事件！");
        }

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/test-mq-flood")
    public ResponseEntity<String> testMqFloodBackpressure() {
        log.info("\n=== 🚨 [💥 MQ 洪峰压测启动] 模拟房东批量导入修改，瞬间倾倒 20 条高负载降价消息 ===");

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= 20; i++) {
            String mockPayload = String.format(
                    "{\"propertyId\":%d, \"title\":\"[Flood Test] Brisbane CBD Luxury Living #%d\", \"oldPrice\":850.00, \"newPrice\":500.00, \"suburb\":\"Brisbane\"}",
                    1000L + i, i
            );

            // 生产者（Controller）全速、毫无间隔地向 RabbitMQ 写入消息
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PROPERTY_EXCHANGE,
                    RabbitMQConfig.PROPERTY_ROUTING_KEY,
                    mockPayload
            );
            log.info("📥 [Controller 生产者] 已成功将消息 #{} 灌入 RabbitMQ 物理队列中", i);
        }

        long endTime = System.currentTimeMillis();
        return ResponseEntity.ok("20条重磅降价消息已成功泄洪至 RabbitMQ！Controller发送耗时：" + (endTime - startTime) + "ms");
    }

    @PostMapping("/test-dlq-trigger")
    public ResponseEntity<String> testDlqTrigger() {
        log.info("\n=== 🧪 [DLQ 死信机制单体测试启动] ===");

        // 构造一条故意带有 "BUG" 的非正常改价消息，诱发消费者拒绝
        String poisonMessage = "{" +
                "\"propertyId\": 999, " +
                "\"title\": \"🚨 [BUG TEST] Brisbane Flawed Apartment 🚨\", " +
                "\"oldPrice\": 900.00, " +
                "\"newPrice\": 400.00, " +
                "\"suburb\": \"Brisbane City\"" +
                "}";

        // 生产者全速投递到原有的正常交换机中
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PROPERTY_EXCHANGE,
                RabbitMQConfig.PROPERTY_ROUTING_KEY,
                poisonMessage
        );

        log.info("📥 [Controller 生产者] 故意构造的毒丸消息已成功注入主队列中。");
        return ResponseEntity.ok("死信测试指令已发出，请观察控制台及 RabbitMQ Management 控制台！");
    }

}
