package com.zzy.aurenteasebackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.service.PropertyService;
import org.hibernate.validator.internal.constraintvalidators.hv.Mod11CheckValidator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@WebMvcTest(PropertyController.class)
class PropertyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyService propertyService;

    @Autowired
    private ObjectMapper objectMapper;



    @Test
    void search() throws Exception {
        PropertySearchCriteria criteria = new PropertySearchCriteria();
        criteria.setSuburb("Sunnybank");
        criteria.setState("QLD");
        criteria.setMinPrice(BigDecimal.valueOf(600));

        List<Property> properties = new ArrayList<>();

        Mockito.when(propertyService.searchProperties(Mockito.any(PropertySearchCriteria.class))).thenReturn(properties);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/properties/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criteria)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}