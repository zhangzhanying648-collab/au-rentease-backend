package com.zzy.aurenteasebackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.service.PropertyService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test") // 🌟 核心修复：指定其使用 src/test/resources/application-test.yml 配置
public class IntergrationTest {
    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSearch(){
        PropertySearchCriteria criteria = new PropertySearchCriteria();
        criteria.setSuburb("Sunnybank");
        criteria.setState("QLD");
        criteria.setMinPrice(BigDecimal.valueOf(600));

        List<Property> result = propertyService.searchProperties(criteria);
    }

    @Test
    void testSearch2()
    {
        PropertySearchCriteria criteria = new PropertySearchCriteria();
        criteria.setSuburb("Sunnybank");
        criteria.setState("QLD");
        criteria.setMinPrice(BigDecimal.valueOf(600));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/properties/search",
                criteria,
                String.class);
    // 🌟 打印出真实的 HTTP 状态码（比如是 200, 400 还是 500）
        System.out.println("====== HTTP STATUS CODE ======");
        System.out.println(response.getStatusCode());

        // 🌟 打印出后端返回的真实面目
        System.out.println("====== BODY CONTENT ======");
        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
