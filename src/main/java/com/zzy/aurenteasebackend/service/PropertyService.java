package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


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
            predicates.add( criteriaBuilder.equal(root.get("status"), "AVAILABLE"));

            //动态匹配suburb，不区分大小写，模糊查询
            if (criteria.getSuburb() != null && !criteria.getSuburb().isBlank()) {
                predicates.add( criteriaBuilder.like(
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
}
