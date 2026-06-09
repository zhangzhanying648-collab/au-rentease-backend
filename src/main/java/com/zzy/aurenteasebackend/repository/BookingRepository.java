package com.zzy.aurenteasebackend.repository;

import com.zzy.aurenteasebackend.domain.Booking;
import com.zzy.aurenteasebackend.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository
        extends JpaRepository<Booking,Long>, JpaSpecificationExecutor<Booking> {
    // 1. 根据租客 Email 查询他约了哪些看房行程（用于前端“我的行程”面板）
    List<Booking> findByUserEmail(String userEmail);

    // 2. 根据房源 ID 查询这套房子有多少人约了看（用于房东/中介管理后台）
    List<Booking> findByPropertyId(Long propertyId);
}
