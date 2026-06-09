package com.zzy.aurenteasebackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private Property property;

    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail;

    @Column(name = "lease_term_months", nullable = false)
    private Integer leaseTermMonths = 12;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SUBMITTED";//APPROVED,REJECTED,

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createAt;
}
