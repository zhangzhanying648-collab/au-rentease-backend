package com.zzy.aurenteasebackend.dto;

import com.zzy.aurenteasebackend.document.PropertyExtendDoc;
import com.zzy.aurenteasebackend.domain.Property;
import lombok.*;

// 包装一个联合接收的 RequestBody 载体 DTO
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PropertyCompositePayload {
    private Property basic;             // 走 MySQL
    private PropertyExtendDoc extend;   // 走 MongoDB
}