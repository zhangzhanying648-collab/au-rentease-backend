package com.zzy.aurenteasebackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="properties")
public class Property {
    @Id
    //设置为自动增1
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    //房源类型：house，townhouse，apartment，Studio
    @Column(name = "property_type", nullable = false, length = 50)
    private String propertyType;

//    @Column(name = "price_per_week", nullable = false, columnDefinition = "numeric(10,2)")
    @Column(name = "price_per_week", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerWeek;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    @Column(name = "car_spaces", nullable = false)
    private Integer carSpaces;

    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    @Column(nullable = false, length = 100)
    private String suburb;

    @Column(nullable = false, length = 3)
    private String state;

    @Column(nullable = false, length = 4)
    private String postcode;

    //房源状态：AVALIABLE, RENTED
    @Column(nullable = false, length = 20)
    private String status="AVAILABLE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at=LocalDateTime.now();
}
