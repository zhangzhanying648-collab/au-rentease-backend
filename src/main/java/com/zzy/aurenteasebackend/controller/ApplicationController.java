package com.zzy.aurenteasebackend.controller;

import com.zzy.aurenteasebackend.domain.Application;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.domain.User;
import com.zzy.aurenteasebackend.repository.ApplicationRepository;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.repository.UserRepository;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationRepository applicationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;



    public ApplicationController(ApplicationRepository applicationRepository, PropertyRepository propertyRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> submitApplication(@RequestBody ApplicationRequest request) {
        String currentUserEmail = getCurrentUserEmail();

        Property property=propertyRepository.findById(request.getPropertyId())
                .orElseThrow(()->new IllegalArgumentException("Property Not Found"));

        Application application=new Application();
        application.setUserEmail(currentUserEmail);
        application.setProperty(property);
        application.setLeaseTermMonths(request.getLeaseTermMonths());
        application.setMoveInDate(request.getMoveInDate());

        applicationRepository.save(application);

        return ResponseEntity.ok(Map.of("success", true, "message", "Application submitted successfully!"));

    }

    @Data
    static class ApplicationRequest {
        private Long propertyId;
        private Integer leaseTermMonths;
        private LocalDate moveInDate;
    }

    @GetMapping("/my")
    public ResponseEntity<List<Application>> getMyApplications() {
        // 🔒 安全核心：从 SecurityContext 里摘取当前租客账号
        String currentUserEmail = getCurrentUserEmail();

        System.out.println("🔍 正在为租客捞取租房申请进度，当前用户: " + currentUserEmail);

        // 调用我们在上一步在 ApplicationRepository 里写好的 findByUserEmail 方法
        List<Application> myApplications = applicationRepository.findByUserEmail(currentUserEmail);

        return ResponseEntity.ok(myApplications);
    }

    private String getCurrentUserEmail() {
        String currentUsername = SecurityContextHolder
                .getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));
        String currentUserEmail = user.getEmail();
        return currentUserEmail;
    }
}
