package com.zzy.aurenteasebackend.repository;

import com.zzy.aurenteasebackend.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// 💡 继承 JpaSpecificationExecutor 后，Repository 就解锁了动态条件查询的超能力
@Repository
public interface PropertyRepository
        extends JpaRepository<Property,Long>, JpaSpecificationExecutor<Property> {
}
