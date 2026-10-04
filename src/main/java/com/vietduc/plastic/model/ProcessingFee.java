package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng quản lý Phát sinh gia công — các loại phụ phí cho từng dòng cửa.
 * <p>
 * Ví dụ: CNC, GHÉP CÁNH, CHỈ SOI, PHÀO NỔI, Ô FIX, KHÁC, ...
 * <p>
 * Khi tạo dòng cửa, người dùng chọn 1 hoặc nhiều phát sinh áp dụng cho dòng đó.
 * Phát sinh có thể có {@code unitPrice} gợi ý — nhưng giá thực tế lấy ở dòng cửa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processing_fees")
public class ProcessingFee {

    @Id
    private String id;

    /** Mã ngắn: CNC, GHEP_CANH, CHI_SOI, ... */
    private String code;

    /** Tên hiển thị: "CNC", "Ghép cánh", "Chỉ soi", "Phào nổi", "Ô fix", "Khác", ... */
    private String name;

    /** Mô tả chi tiết (optional). */
    private String description;

    /** Đơn giá gợi ý (có thể = 0 nếu tính theo bộ cửa). */
    private BigDecimal defaultUnitPrice = BigDecimal.ZERO;

    /** Đơn vị tính: "bộ", "cánh", "m²", "lần", ... */
    private String unit = "bộ";

    /** Đang active? */
    private Boolean active = true;

    /** Thứ tự hiển thị (nhỏ trước). */
    private Integer displayOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
