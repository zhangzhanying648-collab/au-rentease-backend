package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;


@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    void searchProperties() {
        PropertySearchCriteria criteria = new PropertySearchCriteria();
        criteria.setSuburb("Sunnybank");
        criteria.setState("QLD");
        criteria.setMinPrice(BigDecimal.valueOf(600));

        Mockito.when(propertyRepository.findAll(Mockito.any(Specification.class))).thenReturn(Collections.emptyList());

        propertyService.searchProperties(criteria);

        //确保搜索时，propertyRepository收到了带着Specification的调用
        Mockito.verify(propertyRepository, Mockito.times(1)).findAll(Mockito.any(Specification.class));
    }
}