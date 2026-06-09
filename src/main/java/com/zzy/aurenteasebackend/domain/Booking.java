package com.zzy.aurenteasebackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🚀 大厂标配：多对一关联。多个预约可以指向同一套房源
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private Property property;

    // 租客的唯一标识：Email / Username (通过安全上下文抓取)
    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail;

    // 预约看房的日期
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    // 预约状态，默认 PENDING。这里你可以直接用 String，也可以像 Role 一样定义一个枚举
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    // 自动审计日志：创建时间
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}