package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Bảng quản lý danh mục trạng thái đơn hàng.
 * <p>
 * Bảng này cho phép user thêm/sửa/xoá trạng thái tuỳ ý qua UI quản trị,
 * ngoài các trạng thái mặc định có sẵn trong enum {@link OrderStatus}.
 * <p>
 * Field {@code code} là khoá duy nhất (uppercase, snake_case) để ánh xạ
 * khi cần liên kết với dữ liệu cũ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "order_statuses")
public class OrderStatusConfig {

    @Id
    private String id;

    /** Mã định danh duy nhất, viết HOA, snake_case. VD: CHO_XAC_NHAN, DA_GIAO, DANG_VAN_CHUYEN. */
    private String code;

    /** Tên hiển thị. VD: "Chờ xác nhận", "Đang vận chuyển". */
    private String name;

    /** Màu Bootstrap (info/success/warning/danger/secondary/primary) hoặc mã màu tuỳ chọn. */
    private String color;

    /** Thứ tự hiển thị (nhỏ trước). */
    private Integer displayOrder = 0;

    /** Đang active? */
    private Boolean active = true;

    /** Trạng thái hoàn thành (dùng để tính deadline/cảnh báo). */
    private Boolean completed = false;

    /** Ghi chú (optional). */
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
