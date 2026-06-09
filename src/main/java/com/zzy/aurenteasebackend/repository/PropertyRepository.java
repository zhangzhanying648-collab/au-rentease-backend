package com.zzy.aurenteasebackend.repository;

import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertyReportDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// 💡 继承 JpaSpecificationExecutor 后，Repository 就解锁了动态条件查询的超能力
@Repository
public interface PropertyRepository
        extends JpaRepository<Property,Long>, JpaSpecificationExecutor<Property> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Property p SET p.status = :status WHERE p.id = :id")
    void updatePropertyStatus(@Param("id") Long id, @Param("status") String status);


}
