package com.zzy.aurenteasebackend.repository;

import com.zzy.aurenteasebackend.document.PropertyExtendDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyExtendRepository extends MongoRepository <PropertyExtendDoc,String>{
    //Spring Data 绝技：根据方法名自动生成查询逻辑
    Optional<PropertyExtendDoc> findByPropertyId(Long propertyId);

    void deleteByPropertyId(Long propertyId);
}
