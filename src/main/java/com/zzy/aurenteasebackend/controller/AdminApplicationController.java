package com.zzy.aurenteasebackend.controller;

import com.zzy.aurenteasebackend.domain.Application;
import com.zzy.aurenteasebackend.domain.enums.PropertyStatus;
import com.zzy.aurenteasebackend.dto.PropertyReportDTO;
import com.zzy.aurenteasebackend.repository.ApplicationRepository;
import com.zzy.aurenteasebackend.service.ApplicationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *我们在控制器层开辟一条专门供房东/中介使用的后台管理通路 /api/admin/applications
 * 通过 Spring Security 提供的 @PreAuthorize 注解，我们可以在方法入口处直接架设权限机枪，只允许角色为 LANDLORD 或 ADMIN 的账户访问
 */
@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationController {
    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;

    @Data
    static class ApprovalRequest {
        private String status; // 前端传回 "APPROVED" 或 "REJECTED"
        private String comment; // 房东填写的批注
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<?> approveOrRejectApplication(
            @PathVariable Long id,
            @RequestBody ApprovalRequest request) {
        try{
            Application updatedApplication=applicationService.reviewApplication(id, request.getStatus());
            return ResponseEntity.ok(Map.of(
                    "success",true,
                    "message","Application status updated to " + updatedApplication.getStatus() + " successfully!",
                    "currentStatus", updatedApplication.getStatus()
                    ));
        }catch (IllegalStateException | IllegalArgumentException e){
            // 捕获业务层抛出的防御性断言，优雅返回 400 坏请求
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取全量租房申请列表
     * @return
     */
    @GetMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<?> getAllApplications(){
        try {
            List<Application> list = applicationService.getAllApplications();
            return ResponseEntity.ok(list);
        }catch(Exception e){
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to retrieve applications: " + e.getMessage()
            ));
        }
    }

//    @GetMapping("/report")
//    public ResponseEntity<PropertyReportDTO> getFinanceReport() {
//        return ResponseEntity.ok(applicationRepository.getFinanceAndLeaseReport());
//    }

    /**
     * 对于已经成功变更为 RENTED 状态的房源，中介需要掌握整体的租金流和租期分布。     *
     * 功能包含：利用 JPQL 的聚合查询（SUM, COUNT），统计当前总共租出去了多少套
     * 房子、每周产生的总租金流水流水（Gross Weekly Revenue）、以及平均租期长度。
     * @return
     */
    @GetMapping("/report")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<PropertyReportDTO> getFinanceAndLeaseReport(){
        return ResponseEntity.ok(applicationRepository.getFinanceAndLeaseReport("APPROVED", PropertyStatus.RENTED));
    }
}
