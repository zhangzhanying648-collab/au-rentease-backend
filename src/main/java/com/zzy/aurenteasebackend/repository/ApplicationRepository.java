package com.zzy.aurenteasebackend.repository;

import com.zzy.aurenteasebackend.domain.Application;
import com.zzy.aurenteasebackend.domain.enums.PropertyStatus;
import com.zzy.aurenteasebackend.dto.PropertyReportDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUserEmail(String userEmail);

    // 💡 让数据库帮你过滤：根据房源ID、排除当前申请ID、且状态为 SUBMITTED
    List<Application> findByPropertyIdAndIdNotAndStatus(Long propertyId, Long id, String status);

    @Query("select new com.zzy.aurenteasebackend.dto.PropertyReportDTO(" +
            "count(distinct p.id)," +
            "sum(p.pricePerWeek)," +
            "avg(a.leaseTermMonths)) " +
            "from Application a join a.property p where " +
            "a.status=:applicationStatus and p.status=:propertyStatus")
    PropertyReportDTO getFinanceAndLeaseReport(
            @Param("applicationStatus")String applicationStatus,
            @Param("propertyStatus") PropertyStatus propertyStatus);
}