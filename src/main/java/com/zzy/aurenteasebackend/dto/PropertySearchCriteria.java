package com.zzy.aurenteasebackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertySearchCriteria {
    //基础地理筛选
    private String suburb;
    private String state;

    //租金范围筛选
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    //房屋配置筛选
    private Integer minBedrooms;
    private Integer maxBedrooms;

    //房源类型(house townhouse apartment studio)
    private String propertyType;
}
