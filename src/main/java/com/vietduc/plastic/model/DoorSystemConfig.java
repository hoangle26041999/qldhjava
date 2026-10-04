package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Bảng quản lý Dòng cánh (hệ cửa: 40, 45, 50, ...).
 * <p>
 * Khác với enum {@link DoorSystem} cố định — bảng này cho phép user
 * thêm/sửa/xoá hệ cửa tuỳ ý qua UI quản trị.
 * <p>
 * Field {@code isDefault} = true sẽ được dùng làm mặc định khi tạo dòng cửa mới.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "door_systems")
public class DoorSystemConfig {

    @Id
    private String id;

    /** Tên hiển thị: "Hệ 40", "Hệ 45", "Hệ 50", ... */
    private String name;

    /** Mô tả chi tiết (optional): "Khung nhôm hệ 40mm, dùng cho cửa trong nhà"... */
    private String description;

    /** Là hệ mặc định? (chỉ 1 cái được true) */
    private Boolean isDefault = false;

    /** Đang active? */
    private Boolean active = true;

    /** Thứ tự hiển thị (nhỏ trước). */
    private Integer displayOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
