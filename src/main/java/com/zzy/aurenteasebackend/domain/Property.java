package com.zzy.aurenteasebackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zzy.aurenteasebackend.domain.enums.PropertyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="properties")
@DynamicUpdate
public class Property {
    @Id
    //设置为自动增1
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    //房源类型：house，townhouse，apartment，Studio
    @Column(name = "property_type", nullable = false, length = 50)
    private String propertyType="";

//    @Column(name = "price_per_week", nullable = false, columnDefinition = "numeric(10,2)")
    @Column(name = "price_per_week", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerWeek;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(name = "bathrooms", columnDefinition = "numeric")
    private Double bathrooms;

    @Column(name = "car_spaces", nullable = false)
    private Integer carSpaces;

    @Column(name = "street_address", nullable = false)
    private String streetAddress="";

    @Column(nullable = false, length = 100)
    private String suburb="";

    @Column(nullable = false, length = 3)
    private String state="";

    @Column(nullable = false, length = 4)
    private String postcode="";

    //房源状态：AVALIABLE, RENTED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyStatus status= PropertyStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at=LocalDateTime.now();

    @Column(name = "description", nullable = false, length = 1000)
    private String description="";

    @Column(name = "image_url")
    private String imageUrl="";

    // 🚀 核心新增：一套房源对应多个看房预约
// mappedBy = "property" 代表由 Booking 实体类中的 property 属性来维护外键关系
// CascadeType.ALL 代表如果删除了这套房源，底下的预约也一并连带清理，防止产生垃圾数据
//    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
////    @com.fasterxml.jackson.annotation.JsonIgnore // 💡 极重要：防止 Jackson 序列化时死循环陷入无尽黑洞
//    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})//极重要：防止 Jackson 序列化时死循环陷入无尽黑洞
//    @ToString.Exclude
//    private java.util.List<Booking> bookings = new java.util.ArrayList<>();
}
