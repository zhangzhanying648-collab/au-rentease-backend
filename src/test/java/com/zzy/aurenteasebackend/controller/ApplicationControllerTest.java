package com.zzy.aurenteasebackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.aurenteasebackend.domain.Application;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.repository.ApplicationRepository;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationRepository applicationRepository;

    @MockBean
    private PropertyRepository propertyRepository;

    @Test
    @DisplayName("提交租房申请-成功的场景测试")
    //伪造一个已经登录的合规租客，用户名/Email 为 tenant.alex@aurentease.com.au
    @WithMockUser(username="tenant.alex@aurentease.com.au",roles={"TENANT"})
    void submitApplication() throws Exception {
        Long mockPropertyId = 100L;
        Property mockProperty = new Property();
        mockProperty.setId(mockPropertyId);
        mockProperty.setTitle("Apartment in Brisbane CBD");

        Mockito.when(propertyRepository.findById(mockPropertyId))
                .thenReturn(Optional.of(mockProperty));
        Mockito.when(applicationRepository.save(Mockito.any(Application.class)))
                .thenReturn(new Application());

        Map<String,Object> requestPayload= new HashMap<>();
        requestPayload.put("propertyId",mockPropertyId);
        requestPayload.put("leaseTermMonths",12);
        requestPayload.put("moveInDate", LocalDate.now().plusDays(7).toString());

        mockMvc.perform(
                post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application submitted successfully!"));
    }

    @Test
    @DisplayName("⚠️ 递交租房申请 - 房源不存在失败场景测试")
    @WithMockUser(username = "tenant.alex@aurentease.com.au")
    void submitApplication_propertyNotFound() throws Exception {
        // 1. 制造一个空挡：查无此房
        Long nonExistentPropertyId = 999L;
        Mockito.when(propertyRepository.findById(nonExistentPropertyId))
                .thenReturn(Optional.empty());

        // 2. 请求载荷
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("propertyId", nonExistentPropertyId);
        requestPayload.put("leaseTermMonths", 6);
        requestPayload.put("moveInDate", "2026-07-01");

        // 3. 轰炸接口并断言
        // 注意：因为你在 ApplicationController 里用了 .orElseThrow(() -> new IllegalArgumentException(...))
        // 如果你的项目没有写全局异常处理器（@RestControllerAdvice），Spring 默认会吐回 500 或 400。
        // 这里预期它因为异常而阻断
        Exception exception = assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/applications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestPayload)));
        });

        // 4. 精准验证异常的底层原因是不是你写的 "Property Not Found"
        Throwable rootCause = exception.getCause();
        assertEquals(IllegalArgumentException.class, rootCause.getClass());
        assertEquals("Property Not Found", rootCause.getMessage());
    }

    @Test
    @DisplayName("❌ 递交租房申请 - 匿名用户未登录拦截测试")
        // 💡 战术注意：这里故意*不加* @WithMockUser，模拟无牌黑户裸奔请求
    void submitApplication_Anonymous_ReturnsUnauthorized() throws Exception {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("propertyId", 1L);
        requestPayload.put("leaseTermMonths", 12);
        requestPayload.put("moveInDate", "2026-06-20");

        // 裸奔请求应该直接被你的 SecurityConfig 阻挡在外，连 Controller 的大门都进不去
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPayload)))
                .andExpect(status().isUnauthorized()); // 🔒 严格断言返回 401 Unauthorized
    }
}