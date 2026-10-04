package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Một dòng sản phẩm trong bảng báo giá đơn hàng (Order Line Item).
 * <p>
 * Mỗi đơn hàng có nhiều dòng (lineItems), 3 loại:
 *   1. DOOR       — Bộ cửa, có tính kỹ thuật (có size)
 *   2. ACCESSORY  — Khóa, bản lề, kính
 *   3. SERVICE    — Lắp đặt, vận chuyển
 * <p>
 * Field chung: productCode, productName, image, quantity, unit, unitPrice, totalPrice.
 * Field chỉ dành cho DOOR: doorSystem, doorType, patterns, colorName, glassType,
 *                          openingWidth, openingHeight, wallThickness,
 *                          productionSpec, leafWidth/Height, frame, trim...
 * Field chỉ dành cho ACCESSORY: accessoryType (LOCK/HINGE/GLASS).
 * Field chỉ dành cho SERVICE: description.
 * <p>
 * Dùng 1 class duy nhất (không tách) để render bảng phẳng và lưu Mongo đơn giản.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineItem {

    /** Mã sản phẩm (DT01, KHOA-FT02, BL-4T1, LAP-DAT...). */
    private String productCode;

    /** Loại sản phẩm (DOOR / ACCESSORY / SERVICE). */
    private ProductCategory category = ProductCategory.DOOR;

    /** Tên hiển thị (Cửa đi mở trơn, Khóa tròn Inox, Bản lề 4T, Lắp đặt...). */
    private String productName;

    /** URL ảnh sản phẩm (upload bởi user hoặc default). */
    private String image;

    /** Số lượng. */
    private Integer quantity = 1;

    /** Đơn vị tính (bộ, cái, chiếc, lần). */
    private String unit = "bộ";

    /** Đơn giá (VND). */
    private BigDecimal unitPrice;

    /** Thành tiền (= quantity × unitPrice). */
    private BigDecimal totalPrice;

    // ============ Chỉ dành cho DOOR ============
    private DoorSystem doorSystem;
    private DoorType doorType;
    private DoorPattern pattern;           // primary pattern (backward-compat)
    private String colorName;
    private GlassType glassType;
    private Integer openingWidth;
    private Integer openingHeight;
    private Integer wallThickness;

    /** Mã màu (mã nội bộ cho từng bộ cửa — VD: X-01, V-02, T-03, KHAC). */
    private String colorCode;

    // ============ Kết quả kỹ thuật DOOR (auto) ============
    private ProductionSpec productionSpec;
    private Integer leafWidth;
    private Integer leafHeight;
    private Integer verticalFrame;
    private Integer horizontalFrame;
    private Integer heightFrame;
    private Integer verticalTrim;
    private Integer verticalTrimQuantity;
    private Integer horizontalTrim;
    private Boolean needsReview;

    // ============ Phụ phí cho DOOR (checkbox trong bảng báo giá) ============
    /** Ghép cánh (+Bộ). */
    private Boolean extraBo = false;
    /** CNC. */
    private Boolean extraCnc = false;
    /** Phào nổi. */
    private Boolean extraMolding = false;

    // ============ Phát sinh gia công (tham chiếu tới bảng processing_fees) ============
    /** Danh sách ID các phát sinh gia công đã chọn cho dòng cửa này. */
    private List<String> processingFeeIds;
    /** Snapshot tên phát sinh (lưu nhanh để hiển thị, không phải lookup lại). */
    private List<String> processingFeeNames;
    /** Tổng phụ phí gia công cộng dồn (sum defaultUnitPrice của các phát sinh). */
    private java.math.BigDecimal processingFeeAmount = java.math.BigDecimal.ZERO;

    /**
     * Override lưu riêng từng phát sinh gia công (client có thể sửa số tiền từng fee).
     * Không bắt buộc — nếu null thì server tự tính từ defaultUnitPrice × quantity.
     */
    private List<ProcessingFeeEntry> processingFeeEntries;

    /** Ghi chú cho từng dòng. */
    private String notes;

    // ============ Chỉ dành cho ACCESSORY ============
    /** LOCK / HINGE / GLASS / OTHER. */
    private String accessoryType;
    private String accessorySpec;  // "Khóa tròn Inox", "BL 4T mạ đồng"...

    // ============ Chỉ dành cho SERVICE ============
    /** "Lắp đặt", "Vận chuyển", "Sơn cửa"... */
    private String serviceType;
    /** Mô tả chi tiết dịch vụ. */
    private String serviceSpec;

    /** Vị trí lắp đặt (cho DOOR). */
    private DoorLocation location;
    private String locationDetail;

    // ============ Helpers ============
    public BigDecimal calculateTotalPrice() {
        if (quantity == null || unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal safeTotalPrice() {
        if (totalPrice != null) return totalPrice;
        return calculateTotalPrice();
    }

    public boolean isDoor() {
        return category == ProductCategory.DOOR;
    }

    public boolean isAccessory() {
        return category == ProductCategory.ACCESSORY;
    }

    public boolean isService() {
        return category == ProductCategory.SERVICE;
    }

    public String getEffectiveCode() {
        return productCode != null ? productCode : "?";
    }

    public String getEffectiveName() {
        if (productName != null && !productName.isBlank()) return productName;
        if (isDoor() && doorType != null) return doorType.getDisplayName();
        if (isAccessory() && accessorySpec != null) return accessorySpec;
        if (isService() && serviceType != null) return serviceType;
        return "Sản phẩm";
    }

    public String getEffectiveImage() {
        if (image != null && !image.isBlank()) return image;
        // default placeholder theo loại
        if (isDoor()) return "/images/default-door.png";
        if (isAccessory()) return "/images/default-accessory.png";
        if (isService()) return "/images/default-service.png";
        return "/images/default-product.png";
    }

    /**
     * Lấy danh sách tên phát sinh gia công đã chọn (an toàn null).
     */
    public List<String> getProcessingFeeNamesSafe() {
        return processingFeeNames != null ? processingFeeNames : java.util.Collections.emptyList();
    }

    /**
     * Lấy danh sách ID phát sinh gia công đã chọn (an toàn null).
     */
    public List<String> getProcessingFeeIdsSafe() {
        return processingFeeIds != null ? processingFeeIds : java.util.Collections.emptyList();
    }

    /**
     * Lấy ra entry cho 1 feeId (an toàn null).
     */
    public ProcessingFeeEntry getProcessingFeeEntry(String feeId) {
        if (processingFeeEntries == null || feeId == null) return null;
        for (ProcessingFeeEntry e : processingFeeEntries) {
            if (feeId.equals(e.getFeeId())) return e;
        }
        return null;
    }

    /**
     * Một phát sinh gia công được chọn cho dòng sản phẩm (override có thể sửa số tiền).
     * Snapshot lưu vào OrderLineItem để không phải lookup lại lịch sử ProcessingFee sau này.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingFeeEntry {
        /** ID của ProcessingFee. */
        private String feeId;
        /** Tên snapshot (CNC, Ghép cánh, Chỉ số, Phụ nổi...). */
        private String feeName;
        /** Số tiền phụ phí cho 1 dòng (đã nhân SL — cùng đơn vị với totalPrice). */
        private java.math.BigDecimal amount;
        /** Đơn vị tính gốc của fee (bộ, m², cánh...). */
        private String unit;
    }
}