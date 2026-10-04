package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Bảng màu cửa nhựa của nhà máy Việt Đức.
 * <p>
 * Mỗi màu có mã ngắn (V01..V21), tên hiển thị, mã màu hex để vẽ swatch,
 * và cờ active để ẩn/hiện khi chọn màu trong đơn hàng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "color_palette")
public class ColorPalette {

    @Id
    private String id;

    /** Mã màu ngắn (V01..V21). */
    private String code;

    /** Tên hiển thị (VD: "Vân gỗ nâu", "Trắng sứ", "Xám đá", ...). */
    private String name;

    /** Mã màu hex (#ffffff, #c2a378, ...). Dùng để hiển thị swatch. */
    private String hexColor = "#cccccc";

    /** Nhóm màu: "Vân gỗ", "Trơn", "Vân đá", "Đặc biệt", ... */
    private String groupName;

    /** Ghi chú (mô tả thêm, dùng cho showroom, bảng giá). */
    private String notes;

    /** Đang dùng? */
    private Boolean active = true;

    /** Thứ tự hiển thị (nhỏ trước). */
    private Integer displayOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}