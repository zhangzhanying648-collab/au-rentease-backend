package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import jakarta.persistence.Column;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

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

    public Optional<Property> getPropertyById(Long id) {
        return propertyRepository.findById(id);
    }

    /**
     * 修改/保存资产档案的工业级实现
     */
    @Transactional
    public Property saveOrUpdateProperty(Long id, Property dto) {
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
        return propertyRepository.save(property);
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
}

