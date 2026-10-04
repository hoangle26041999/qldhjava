package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tính toán kỹ thuật cho đơn hàng.
 * Hệ thống tự động tính dựa trên ô chờ, dày tường, hệ cửa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionSpec {

    // ============ Kích thước ô chờ (nhập tay) ============
    private Integer openingWidth;   // Rộng ô chờ (mm)
    private Integer openingHeight;  // Cao ô chờ (mm)
    private Integer wallThickness;   // Dày tường D (mm)
    private Integer quantity;        // Số bộ cửa (mặc định 1)

    // ============ Kích thước cánh sản xuất (tự động) ============
    private Integer leafWidth;      // Rộng cánh SX
    private Integer leafHeight;     // Cao cánh SX

    // ============ Khuôn đứng (theo dày tường, hệ) ============
    private Integer verticalFrame;   // Khuôn đứng (mm)
    private Boolean verticalFrameCustom; // đánh dấu tùy chỉnh

    // ============ Khuôn ngang (theo R ô chờ) ============
    private Integer horizontalFrame; // Khuôn ngang (mm)
    private Boolean horizontalFrameCustom;

    // ============ Khuôn cao (theo C ô chờ) ============
    private Integer heightFrame;    // Khuôn cao (mm)
    private Boolean heightFrameCustom;

    // ============ Nẹp đứng (theo chiều cao cánh SX) ============
    private Integer verticalTrim;    // Nẹp đứng (mm)
    private Integer verticalTrimQuantity; // số thanh = bộ × 4
    private Boolean verticalTrimCustom;

    // ============ Nẹp ngang (theo R ô chờ) ============
    private Integer horizontalTrim;  // Nẹp ngang (mm)
    private Boolean horizontalTrimCustom;

    // ============ Nẹp chống hở (cho bộ gió / cửa đôi) ============
    private Integer antiGapTrimQuantity; // số thanh nẹp chống hở

    // ============ Trạng thái xác nhận ============
    private Boolean needsReview;     // cần KTO xác nhận tùy chỉnh

    public int safeQuantity() {
        return quantity != null && quantity > 0 ? quantity : 1;
    }
}