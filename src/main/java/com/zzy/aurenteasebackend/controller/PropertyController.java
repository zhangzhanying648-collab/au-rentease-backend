package com.zzy.aurenteasebackend.controller;

import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.dto.PropertySearchCriteria;
import com.zzy.aurenteasebackend.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {
    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // 🛡️ 权限大闸：系统执行这个方法前，会自动去检查当前用户的 authorities 集合里有没有 "ROLE_TENANT"
    // 如果没有（比如一个未登录用户或者房东撞进来了），直接拦截并返回 403 Forbidden（拒绝访问）！
//    @PreAuthorize("hasRole('TENANT')")
    // 💡 澳洲大厂规范：检索类接口一律用 GET 请求，通过 QueryString 传参
    @PostMapping("/search")
    @GetMapping("/search")
    public ResponseEntity<List<Property>> search(PropertySearchCriteria criteria) {
        List<Property> result = propertyService.searchProperties(criteria);
        return ResponseEntity.ok(result);
    }
}
