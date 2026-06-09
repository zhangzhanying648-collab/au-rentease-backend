package com.zzy.aurenteasebackend.domain.enums;

import lombok.Getter;

@Getter
public enum PropertyStatus {
    AVAILABLE("Available", "房源空闲，可接受申请"),
    UNDER_REVIEW("Under Review", "申请审核中"),
    RENTED("Rented", "已成功出租"),
    MAINTENANCE("Maintenance", "下线维护中");

    private final String code;
    private final String description;

    // 构造函数
    PropertyStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
