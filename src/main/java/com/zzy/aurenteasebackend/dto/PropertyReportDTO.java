package com.zzy.aurenteasebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PropertyReportDTO {
    private Long totalRentedProperties;
    private BigDecimal grossWeeklyRevenue;
    private Double averageLeaseTerm;

    // 提供全参构造器，供 JPQL 投影使用
    public PropertyReportDTO(Long totalRentedProperties, BigDecimal grossWeeklyRevenue, Double averageLeaseTerm) {
        this.totalRentedProperties = totalRentedProperties != null ? totalRentedProperties : 0L;
        // 防御空值：如果没有任何房子租出去，SUM(p.pricePerWeek) 会返回 null
        this.grossWeeklyRevenue = grossWeeklyRevenue != null ? grossWeeklyRevenue : BigDecimal.ZERO;
        this.averageLeaseTerm = averageLeaseTerm != null ? averageLeaseTerm : 0.0;
    }
}
