package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Một BỘ cửa cụ thể trong MẪU cửa (DoorPatternGroup).
 * <p>
 * Nhiều bộ cửa có thể dùng chung 1 mẫu (cùng doorType/patterns/system)
 * nhưng khác vị trí lắp đặt, khác kích thước ô chờ, khác số lượng khóa.
 * <p>
 * ProductionSpec được tính riêng cho từng bộ (dựa vào size của bộ + parameters của mẫu).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoorItem {

    /** Mã riêng cho bộ (VD-DH001-MAU01-CUA01). */
    private String itemCode;

    /** Vị trí lắp đặt (phòng ngủ, WC, bếp...). */
    private DoorLocation location;

    /** Mô tả vị trí chi tiết (nhập tay): "Phòng ngủ master tầng 2". */
    private String locationDetail;

    // ============ Kích thước ô chờ (mm) - RIÊNG cho bộ này ============
    private Integer openingWidth;         // Rộng ô chờ
    private Integer openingHeight;        // Cao ô chờ
    private Integer wallThickness;        // Dày tường D

    // ============ Phụ kiện (riêng cho bộ) ============
    private String lockType;              // Loại khóa
    private Integer lockQuantity;         // Số lượng khóa
    private String hingeType;             // Loại bản lề
    private Integer hingeQuantity;        // Số lượng bản lề
    private Integer glassQuantity;        // Số lượng ô kính

    /** Mã màu cho bộ cửa (VD: null, "X-01", "V-02", "T-03"). */
    private String colorCode;

    // ============ Kết quả kỹ thuật (tự động tính, lưu nhanh) ============
    private ProductionSpec productionSpec;

    private Integer leafWidth;            // Cánh R SX
    private Integer leafHeight;           // Cánh C SX
    private Integer verticalFrame;        // Khuôn đứng
    private Integer horizontalFrame;      // Khuôn ngang
    private Integer heightFrame;          // Khuôn cao
    private Integer verticalTrim;         // Nẹp đứng
    private Integer verticalTrimQuantity; // SL nẹp đứng
    private Integer horizontalTrim;       // Nẹp ngang
    private Boolean needsReview;          // cần KTO xác nhận

    // ============ Giá riêng cho bộ (optional) ============
    private BigDecimal unitPrice;

    /** Số lượng bộ giống nhau (default = 1). Khi > 1, service sẽ tạo N Order cùng spec. */
    private Integer quantity = 1;

    /** Tạo mã bộ cửa tự động (gọi từ service khi thêm mới). */
    public static String generateItemCode(String groupCode, int index) {
        return groupCode + "-CUA" + String.format("%02d", index);
    }

    public int safeVerticalTrimQuantity() {
        return verticalTrimQuantity != null ? verticalTrimQuantity : 0;
    }
}