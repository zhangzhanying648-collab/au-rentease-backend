package com.zzy.aurenteasebackend.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

/**
 * 非关系型的文档类
 * 它通过 propertyId 与你现有的 MySQL Property 实体进行跨库关联。
 * amenities 字段是一个极其自由的 Map<String, Object>，房东可以随意添加任何奇葩的设备标签，彻底解放 MySQL。
 *
 * 房源详情的“动态属性”（Dynamic Attributes）：不同类型的房源，其配置差异巨大。公寓（Apartment）需要记录：
 * 是否有车位、有无电梯、物业费、是否有公共泳池、前台安保情况；而独栋别墅（House）需要记录：后院面积、是否带围栏、
 * 是否允许养宠物、雨水收集箱容量、太阳能板瓦数。如果用 MySQL，你要么设计大量空置字段，要么搞复杂的 EAV（Entity-Attribute-Value）表，
 * 导致联表查询性能雪崩。而 MongoDB 的 无模式（Schema-less） 特性允许一条记录直接存成多层嵌套的 JSON 字典。
 */
@Data
@Document(collection = "property_extends")// 指定存储在 MongoDB 的哪个集合（类似表名）
public class PropertyExtendDoc {
    // MongoDB 默认的 ObjectId 字符串
    @Id
    private String id;

    @Indexed(unique = true) // 🌟 建立唯一索引，与 MySQL 中的 propertyId 一一对应
    @Field("property_id")
    private Long propertyId;

    // 1. 数组类型：存放房源的亮点标签（例如：["近火车站", "包家具", "阳光充足"]）
    private List<String> highlights;

    // 2. 核心大招：动态 Map 类型（动态属性天花板）
    // 公寓就存: {"hasPool": true, "intercom": "智能对讲"}
    // 别墅就存: {"backyardSize": "50sqm", "petFriendly": "仅限小狗"}
    private Map<String, Object> amenities;

    // 3. 多层嵌套模型：存放房东自定义的招租偏好（Preferences）
    private LeasePreference preference;

    @Data
    public static class LeasePreference {
        private Integer minLeaseMonths; // 最短租期
        private Boolean preferStudent;   // 是否学生优先
        private String availableFrom;   // 可入住日期（如 "2026-07-01"）
    }

}
