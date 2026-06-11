package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.document.PropertyExtendDoc;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.repository.PropertyExtendRepository;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.websocket.NotificationWebSocketHandler;
import jakarta.persistence.Column;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class PropertyService {
    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);

    private final PropertyRepository propertyRepository;

    private final PropertyExtendRepository propertyExtendRepository;

    private final RedisTemplate<String, Object> redisTemplate; // 🌟 注入我们的 JSON Redis 模板

    private static final String CACHE_KEY_PREFIX = "rentease:property:";



    public List<Property> searchProperties(PropertySearchCriteria criteria) {
        //root是实体类Property
        //criteriaBuilder是操作符工厂：equal，like，greaterthan
        //query用来出来group by， order by
        Specification<Property> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            //只展示可租的房源
            predicates.add(criteriaBuilder.equal(root.get("status"), "AVAILABLE"));

            //动态匹配suburb，不区分大小写，模糊查询
            if (criteria.getSuburb() != null && !criteria.getSuburb().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("suburb")),
                        "%" + criteria.getSuburb().toLowerCase() + "%"
                ));
            }

            //动态匹配州
            if (criteria.getState() != null && !criteria.getState().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("state"),
                        criteria.getState().toUpperCase()
                ));
            }

            //租金范围:大于等于最小租金
            if (criteria.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("pricePerWeek"), criteria.getMinPrice()));
            }

            //小于等于maxprice
            if (criteria.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("pricePerWeek"), criteria.getMaxPrice()));
            }

            //至少X个卧室
            if (criteria.getMinBedrooms() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bedrooms"), criteria.getMinBedrooms()));
            }

            if (criteria.getMaxBedrooms() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("bedrooms"), criteria.getMaxBedrooms()));
            }

            if (criteria.getPropertyType() != null && !criteria.getPropertyType().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("propertyType"), criteria.getPropertyType()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return propertyRepository.findAll(specification);
    }

    /**
     * 🌟 1. 读请求运用 Redis：经典的“旁路缓存（Cache-Aside Pattern）”
     */
    public Property getPropertyById(Long id) {
//        return propertyRepository.findById(id);
        String redisKey = CACHE_KEY_PREFIX + id;
        // 🔍 第一步：先去 Redis 缓存里摸底
        Property cachedProperty = (Property) redisTemplate.opsForValue().get(redisKey);
        if (cachedProperty != null) {
            log.info("🎯 [Redis 命中] 租客正在查看房源 #{}, 直接从 Redis 返回 JSON 缓存", id);
            return cachedProperty;
        }

        // 🔍 第二步：Redis 没捞到，穿透去 MySQL 捞
        log.warn("💾 [Redis 未命中] 房源 #{} 的缓存失效或首次访问，正在穿透至 MySQL 查询...", id);
        Property mysqlProperty = propertyRepository.findById(id).orElse(null);

        // 🔍 第三步：将 MySQL 捞出来的果实，顺手栽进 Redis，并设置过期时间（防止缓存雪崩/僵尸缓存）
        if (mysqlProperty != null) {
            // 设置 1 小时随机过期时间，防止大量缓存同一时刻集体失效导致雪崩
            long expireTime = 60 + java.util.concurrent.ThreadLocalRandom.current().nextLong(30);
            redisTemplate.opsForValue().set(redisKey, mysqlProperty, expireTime, TimeUnit.MINUTES);
            log.info("📥 [Redis 补偿] 已将房源 #{} 的最新数据回填至 Redis, 有效期 {} 分钟", id, expireTime);
        }

        return mysqlProperty;
    }

    /**
     * 修改/保存资产档案的工业级实现
     */
    @Transactional
    public Property saveOrUpdateProperty(Long id, Property dto) {
        // 如果是修改操作，为了防止读写并发引发脏数据，执行“先删缓存，再写库”或“写库成功，再删缓存”
        if (id != null) {
            String redisKey = CACHE_KEY_PREFIX + id;
            redisTemplate.delete(redisKey);
            log.info("🗑️ [Redis 一致性] 房东正在修改房源 #{}, 预先清除旧缓存大闸", id);
        }

        Property property;

        // 1. 判断是新增还是更新操作
        if (id == null) {
            // 🚀 情况 A：id 为空，代表房东点击了 "Publish Asset" (新增房源)
            property = new Property();
        } else {
            // 🚀 情况 B：id 有值，代表房东点击了 "Save Changes" (修改现有房源)
            property = propertyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("未找到对应的房源资产，无法执行更新，ID: " + id));
        }

        if (id == null) {
            BeanUtils.copyProperties(dto, property, "id");
        } else {
            // 🚀 更新操作：只拷贝不为空（且非空字符串）的字段！一行代码解决战斗
            BeanUtils.copyProperties(dto, property, getNullOrEmptyPropertyNames(dto));
        }


        // 🌟 核心落库点：更新或写入 S3 的唯一 ObjectKey 钥匙
        if (dto.getImageUrl() != null) {
            property.setImageUrl(dto.getImageUrl());
        }

        // 3. 执行物理保存：
        // 如果是全新 new 出来的，这里会发出 INSERT 语句
        // 如果是从 findById 捞出来的，这里会发出 UPDATE 语句
        Property savedProperty = propertyRepository.save(property);

        // 🚀 核心动作：写库成功后，再次清除缓存（双删策略，防御极端的并发写干扰）
        String redisKey = CACHE_KEY_PREFIX + savedProperty.getId();
        redisTemplate.delete(redisKey);
        log.info("🗑️ [Redis 一致性] 房源 #{} MySQL 写入成功，二次清除缓存，确保租客下一次必读最新数据", savedProperty.getId());

        return savedProperty;
    }

    /**
     * 💡 大厂标配反射工具：提取对象中所有 null 以及空字符串的属性名称
     */
    private String[] getNullOrEmptyPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        // 把 id 排除在拷贝范围外，防止前端传入错误的 id 或没有传 id 导致解包错误
        emptyNames.add("id");

        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            // 核心防御：如果字段是 null，或者字段是字符串且剔除空格后为空，就加入“忽略拷贝大礼包”
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            } else if (srcValue instanceof String && ((String) srcValue).trim().isEmpty()) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames.toArray(new String[0]);
    }

    /**
     * 🌟 1. 写请求：大厂多源异构数据库双写实践
     */
    @Transactional
    public Property savePropertyWithMongoExtend(Property basicDto, PropertyExtendDoc extendDto){
        log.info("💾 [跨库写入] 1. 优先开始持久化房源基础结构至 MySQL...");
        Property savedProperty = propertyRepository.save(basicDto);
        Long propertyId = savedProperty.getId();

        log.info("🍃 [跨库写入] 2. MySQL 主键已生成（#{}），开始组装动态扩展字段投递至 MongoDB...", propertyId);
        PropertyExtendDoc mongoDoc=propertyExtendRepository.findByPropertyId(propertyId)
                .orElse(new PropertyExtendDoc());
        mongoDoc.setPropertyId(propertyId);
        mongoDoc.setHighlights(extendDto.getHighlights());
        mongoDoc.setAmenities(extendDto.getAmenities());
        mongoDoc.setPreference(extendDto.getPreference());

        propertyExtendRepository.save(mongoDoc);
        log.info("✅ [跨库写入] 3. 房源 #{} 的多源异构存储（MySQL 核心表 + MongoDB 动态文档）全部闭环成功！", propertyId);
        return savedProperty;
    }

    /**
     * 🌟 2. 读请求：混合打包聚合输出
     */
    public Map<String, Object> getFullPropertyDetails(Long id) {
        Property basic=propertyRepository.findById(id).orElse(null);
        if(basic==null){
            return null;
        }

        PropertyExtendDoc extend = propertyExtendRepository.findByPropertyId(id).orElse(null);

        return Map.of(
                "id", basic.getId(),
                "title",basic.getTitle(),
                "pricePerWeek", basic.getPricePerWeek(),
                "suburb",basic.getSuburb(),
                "highlights",(extend!=null)?extend.getHighlights():List.of(),
                "amentities",(extend!=null)?extend.getAmenities():Map.of(),
                "preference",(extend!=null)?extend.getPreference():Map.of()
        );
    }
}

