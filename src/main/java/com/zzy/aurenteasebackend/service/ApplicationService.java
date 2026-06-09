package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.domain.Application;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.domain.enums.PropertyStatus;
import com.zzy.aurenteasebackend.repository.ApplicationRepository;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final PropertyRepository propertyRepository;
    private final EmailService emailService;

    /**
     * 审批申请单
     * 1. 防重审批校验：已被处理过的申请无法重复审批。
     * 2. 多单联动清洗：一旦一份申请被批准，同一套房子的其他 SUBMITTED 申请自动归档为 REJECTED，同时房源下架。
     * @param applicationId
     * @param targetStatus
     * @return
     */
    @Transactional
    public Application reviewApplication(Long applicationId, String targetStatus) {
        //1 获取申请单
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application Not Found"));

        //2 防御性校验：如果申请单已经是终态，不允许二次审批。
        if (!"SUBMITTED".equals(application.getStatus())) {
            throw new IllegalStateException("The application has already been processed.");
        }

        //3 校验传入的审批状态是否合规
        if (!"APPROVED".equals(targetStatus) && !"REJECTED".equals(targetStatus)) {
            throw new IllegalStateException("Invalid target status, Must be REJECTED or APPROVED");

        }

        //4 执行桩体变更
        application.setStatus(targetStatus);



        // 5. 核心联动：如果当前申请被中介 [批准 (APPROVED)]
        if ("APPROVED".equals(targetStatus)) {
            Long propertyId = application.getProperty().getId();
            Property property = application.getProperty();
            property.setStatus(PropertyStatus.RENTED);

            //将当前房源状态更新为已出租
//            propertyRepository.updatePropertyStatus(propertyId, "RENTED");

            //获取该房源下正在申请的其他申请单，全部回绝
            /**
             * 这里注释掉的代码是错误的，
             * 如果用findAll，会把已经批准的那条application也包含进来，并且状态再次被覆盖
             */
            List<Application> otherApplications = applicationRepository
                    .findByPropertyIdAndIdNotAndStatus(propertyId, applicationId, "SUBMITTED");

            if (otherApplications.size() > 0) {
                for (Application competitor : otherApplications) {
                    competitor.setStatus("REJECTED");
                }
                applicationRepository.saveAll(otherApplications);
                log.info("房源" + propertyId + "已经成功出租，已联动拒绝其余" + otherApplications.size() + "份竞争申请。");
            }
        }

        Application savedApplication = applicationRepository.save(application);

        //无论是APPROVED还是REJECTED,都发送邮件
        emailService.sendApplicationResultEmail(savedApplication.getUserEmail(),
                savedApplication.getProperty().getTitle(),
                targetStatus);

        return savedApplication;
    }


    public List<Application> getAllApplications(){
        List<Application> list=applicationRepository.findAll();
        return list;
    }
}
