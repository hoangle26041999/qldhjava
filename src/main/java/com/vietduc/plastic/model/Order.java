package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    // Mã đơn hàng tự động
    private String orderCode;

    // ============ Thông tin khách hàng ============
    private String customerName;
    private String customerPhone;
    private String customerAddress;

    // ============ Thông số kỹ thuật cửa (Bước 1) ============
    private DoorSystem doorSystem;        // Hệ 40 / 45
    private DoorType doorType;            // Cửa phòng / chính / WC / ...
    private DoorPattern pattern;          // Trơn / CNC / Phào nổi / Chỉ nhôm
    private String colorName;             // Màu (nhập tay)
    private GlassType glassType;          // Ô kính: ngắn / dài / ô gió / không

    // ============ Kích thước ô chờ (mm) ============
    private Integer openingWidth;         // Rộng ô chờ
    private Integer openingHeight;        // Cao ô chờ
    private Integer wallThickness;        // Dày tường D
    private Integer quantity;             // Số bộ cửa

    // ============ Phụ phí (Bước 2) ============
    private Boolean extraScrew;              // Trơn (bản lề thường)
    private Boolean extraCnc;                // CNC
    private Boolean extraGlassShort;         // Ô kính ngắn
    private Boolean extraGlassLong;          // Ô kính dài
    private Boolean extraVent;               // Ô gió
    private Boolean extraAluminum;           // Chỉ nhôm
    private Boolean extraMolding;            // Phào nổi
    private Boolean extraPaintDoor;          // Sơn cửa
    private Boolean extraBo;                 // Bo
    private Boolean extraMoldingGia;         // Phào nổi giá
    private Boolean extraLock;               // Khóa
    private Boolean extraColor;              // Màu

    // ============ Kết quả kỹ thuật (tự động tính) ============
    private ProductionSpec productionSpec;

    // ============ Phụ kiện (nhập tay) ============
    private String lockType;               // Loại khóa
    private Integer lockQuantity;          // Số lượng khóa
    private String hingeType;              // Loại bản lề
    private Integer hingeQuantity;         // Số lượng bản lề
    private Integer glassQuantity;         // Số lượng ô kính
    private String notes;                  // Ghi chú

    // ============ Kết quả kỹ thuật (lưu nhanh cho truy vấn) ============
    private Integer leafWidth;            // Cánh R SX
    private Integer leafHeight;           // Cánh C SX
    private Integer verticalFrame;        // Khuôn đứng
    private Integer horizontalFrame;      // Khuôn ngang
    private Integer heightFrame;          // Khuôn cao
    private Integer verticalTrim;         // Nẹp đứng
    private Integer verticalTrimQuantity; // SL nẹp đứng
    private Integer horizontalTrim;       // Nẹp ngang
    private Boolean needsReview;          // cần xác nhận

    // ============ Tiền và trạng thái ============
    private BigDecimal totalAmount;       // Giá bán (nhập tay)

    /**
     * Trạng thái đơn hàng (lưu dạng String — code từ bảng order_statuses).
     * Backward-compat: vẫn có getter/setter cũ kiểu enum (deprecated) trả về enum tương ứng nếu khớp.
     */
    private String status;

    // ============ Thời gian ============
    private LocalDateTime orderDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ============ Danh sách các DÒNG sản phẩm trong đơn (Cấu trúc phẳng: CỬA + PHỤ KIỆN + DỊCH VỤ) ============
    private List<OrderLineItem> lineItems;

    // ============ Danh sách các MẪU cửa trong đơn (LEGACY — backward-compat với data cũ) ============
    private List<DoorPatternGroup> patternGroups;

    // ============ Danh sách bộ cửa (legacy flat, optional — dùng cho data cũ) ============
    private List<DoorItem> doors;

    // ============ Helpers: deadline & canh bao (tinh tu orderDate) ============
    public static final int DEFAULT_PRODUCTION_DAYS = 10;
    public static final int WARNING_DAYS = 7;

    /**
     * Ngay hen san xuat xong (= orderDate + 10 ngay).
     */
    public java.time.LocalDateTime getProductionDeadline() {
        java.time.LocalDateTime base = orderDate != null ? orderDate : createdAt;
        return base != null ? base.plusDays(DEFAULT_PRODUCTION_DAYS) : null;
    }

    /**
     * So ngay da qua tu luc dat den hien tai (theo orderDate).
     */
    public long getDaysElapsed() {
        java.time.LocalDateTime base = orderDate != null ? orderDate : createdAt;
        if (base == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(base, java.time.LocalDateTime.now());
    }

    /**
     * So ngay con lai den deadline (am neu qua han).
     */
    public long getDaysRemaining() {
        java.time.LocalDateTime deadline = getProductionDeadline();
        if (deadline == null) return DEFAULT_PRODUCTION_DAYS;
        return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDateTime.now(), deadline);
    }

    /**
     * Don da hoan thanh (HOAN_THANH) hay chua.
     * Logic nghiệp vụ: status = "HOAN_THANH" HOẶC status có completed=true (sẽ check runtime ở service).
     * Tại model, chỉ check hardcode "HOAN_THANH" để tránh phụ thuộc vòng.
     */
    public boolean isCompleted() {
        return "HOAN_THANH".equals(status);
    }

    /**
     * Muc do canh bao:
     *  - NONE: da hoan thanh hoac chua qua 7 ngay
     *  - WARNING: chua hoan thanh, da >= 7 ngay nhung chua qua 10 ngay
     *  - OVERDUE: chua hoan thanh, qua 10 ngay (qua deadline)
     */
    public String getWarningLevel() {
        if (isCompleted()) return "NONE";
        long elapsed = getDaysElapsed();
        if (elapsed >= DEFAULT_PRODUCTION_DAYS) return "OVERDUE";
        if (elapsed >= WARNING_DAYS) return "WARNING";
        return "NONE";
    }

    // ============ Helpers: quản lý danh sách mẫu cửa ============

    /**
     * Đảm bảo patternGroups không null. Khởi tạo list rỗng nếu cần.
     */
    public List<DoorPatternGroup> getPatternGroupsSafe() {
        if (patternGroups == null) {
            patternGroups = new java.util.ArrayList<>();
        }
        return patternGroups;
    }

    /**
     * Lấy tất cả các bộ cửa từ tất cả các mẫu (flat list).
     */
    public List<DoorItem> getAllDoorItems() {
        if (patternGroups == null || patternGroups.isEmpty()) {
            return doors != null ? doors : new java.util.ArrayList<>();
        }
        List<DoorItem> all = new java.util.ArrayList<>();
        for (DoorPatternGroup g : patternGroups) {
            if (g.getDoors() != null) all.addAll(g.getDoors());
        }
        return all;
    }

    /**
     * Tổng số bộ cửa trong đơn.
     */
    public int getTotalDoorCount() {
        if (patternGroups != null && !patternGroups.isEmpty()) {
            return getAllDoorItems().size();
        }
        if (doors != null && !doors.isEmpty()) {
            return doors.size();
        }
        return quantity != null ? quantity : 0;
    }

    /**
     * Đếm số bộ cửa cần KTO xác nhận (needsReview=true).
     */
    public int getDoorNeedsReviewCount() {
        return (int) getAllDoorItems().stream()
                .filter(d -> Boolean.TRUE.equals(d.getNeedsReview()))
                .count();
    }

    /**
     * Đếm số vị trí khác nhau trong đơn.
     */
    public int getUniqueLocationCount() {
        return (int) getAllDoorItems().stream()
                .map(d -> d.getLocation())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    /**
     * Số mẫu cửa trong đơn.
     */
    public int getPatternGroupCount() {
        return patternGroups != null ? patternGroups.size() : 0;
    }

    // ============ Helpers: quản lý lineItems (cấu trúc phẳng) ============

    /**
     * Lấy danh sách lineItems an toàn (không null).
     */
    public List<OrderLineItem> getLineItemsSafe() {
        if (lineItems == null) {
            lineItems = new java.util.ArrayList<>();
        }
        return lineItems;
    }

    /**
     * Lấy chỉ các dòng DOOR.
     */
    public List<OrderLineItem> getDoorLineItems() {
        return getLineItemsSafe().stream()
                .filter(OrderLineItem::isDoor)
                .toList();
    }

    /**
     * Lấy chỉ các dòng ACCESSORY.
     */
    public List<OrderLineItem> getAccessoryLineItems() {
        return getLineItemsSafe().stream()
                .filter(OrderLineItem::isAccessory)
                .toList();
    }

    /**
     * Lấy chỉ các dòng SERVICE.
     */
    public List<OrderLineItem> getServiceLineItems() {
        return getLineItemsSafe().stream()
                .filter(OrderLineItem::isService)
                .toList();
    }

    /**
     * Tổng thành tiền từ tất cả lineItems.
     */
    public BigDecimal calculateLineItemsTotal() {
        return getLineItemsSafe().stream()
                .map(li -> {
                    BigDecimal sub = li.safeTotalPrice() != null ? li.safeTotalPrice() : BigDecimal.ZERO;
                    BigDecimal fee = li.getProcessingFeeAmount() != null ? li.getProcessingFeeAmount() : BigDecimal.ZERO;
                    return sub.add(fee);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Tổng tiền thực sự của đơn (đã gồm phụ phí gia công).
     * Ưu tiên tính lại từ line items (luôn đúng, gồm phụ phí).
     * Fallback về totalAmount nếu đơn không có line items (đơn 1-dòng cũ / manual override).
     */
    public BigDecimal effectiveTotal() {
        if (getLineItemsSafe() != null && !getLineItemsSafe().isEmpty()) {
            return calculateLineItemsTotal();
        }
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    /**
     * Tổng số bộ cửa (đếm DOOR line items, quantity).
     */
    public int getDoorLineItemCount() {
        return getDoorLineItems().stream()
                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 1)
                .sum();
    }

    /**
     * Tổng số bộ cửa theo từng hệ cánh (DoorSystem) — dùng cho bảng tóm tắt sản xuất.
     */
    public Map<DoorSystem, Integer> getDoorSystemSummary() {
        Map<DoorSystem, Integer> map = new EnumMap<>(DoorSystem.class);
        for (DoorSystem s : DoorSystem.values()) map.put(s, 0);
        for (OrderLineItem li : getLineItemsSafe()) {
            if (!li.isDoor()) continue;
            if (li.getDoorSystem() == null) continue;
            int qty = li.getQuantity() != null ? li.getQuantity() : 0;
            map.merge(li.getDoorSystem(), qty, Integer::sum);
        }
        return map;
    }

    /**
     * Tổng số bộ cửa không gán hệ cánh (DoorSystem = null).
     */
    public int getDoorUnassignedCount() {
        return getDoorLineItems().stream()
                .filter(d -> d.getDoorSystem() == null)
                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0)
                .sum();
    }

    /**
     * Safe getters cho view (tránh NullPointerException khi Boolean=null).
     */
    public Boolean getExtraCncSafe() { return extraCnc != null ? extraCnc : false; }
    public Boolean getExtraGlassShortSafe() { return extraGlassShort != null ? extraGlassShort : false; }
    public Boolean getExtraGlassLongSafe() { return extraGlassLong != null ? extraGlassLong : false; }
    public Boolean getExtraVentSafe() { return extraVent != null ? extraVent : false; }
    public Boolean getExtraAluminumSafe() { return extraAluminum != null ? extraAluminum : false; }
    public Boolean getExtraMoldingSafe() { return extraMolding != null ? extraMolding : false; }
    public Boolean getExtraPaintDoorSafe() { return extraPaintDoor != null ? extraPaintDoor : false; }
    public Boolean getExtraBoSafe() { return extraBo != null ? extraBo : false; }
    public Boolean getExtraMoldingGiaSafe() { return extraMoldingGia != null ? extraMoldingGia : false; }
    public Boolean getExtraLockSafe() { return extraLock != null ? extraLock : false; }
    public Boolean getExtraColorSafe() { return extraColor != null ? extraColor : false; }

    /**
     * Số bộ cửa đã hoàn thành (true nếu tất cả đều OK).
     * Hiện tại: trả về status đơn. Sau này có thể track per-door.
     */
    public boolean isAllDoorsCompleted() {
        return isCompleted();
    }

    // ============ Hiển thị phát sinh gia công theo từng bộ cửa ============

    /**
     * Một entry gọn nhẹ cho view: 1 dòng cửa + danh sách phát sinh gia công của nó.
     * Dùng cho trang /orders (danh sách đơn hàng) — gom theo từng bộ cửa.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoorFeeEntry {
        /** Mã dòng cửa (DT01, DT02…). */
        private String productCode;
        /** Tên cửa (mô tả). */
        private String productName;
        /** Số lượng bộ cửa trong dòng này. */
        private Integer quantity;
        /** Danh sách tên phát sinh gia công đã chọn (snapshot). */
        private List<String> processingFeeNames;
        /** Tổng phụ phí gia công cộng dồn (VND). */
        private java.math.BigDecimal processingFeeAmount = java.math.BigDecimal.ZERO;

        /** Có phát sinh gia công hay không. */
        public boolean hasFees() {
            return processingFeeNames != null && !processingFeeNames.isEmpty();
        }
    }

    /**
     * Lấy danh sách các dòng DOOR có phát sinh gia công (chỉ những dòng có chọn fee).
     * Dùng cho view: hiển thị phát sinh gia công theo từng bộ cửa.
     */
    public List<DoorFeeEntry> getDoorFeeEntries() {
        java.util.List<DoorFeeEntry> out = new java.util.ArrayList<>();
        for (OrderLineItem li : getDoorLineItems()) {
            java.util.List<String> names = li.getProcessingFeeNamesSafe();
            out.add(new DoorFeeEntry(
                li.getProductCode(),
                li.getProductName(),
                li.getQuantity(),
                names,
                li.getProcessingFeeAmount() != null ? li.getProcessingFeeAmount() : java.math.BigDecimal.ZERO
            ));
        }
        return out;
    }

    /**
     * Tổng phụ phí gia công của tất cả các dòng cửa (VND).
     */
    public java.math.BigDecimal getTotalProcessingFeeAmount() {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (DoorFeeEntry e : getDoorFeeEntries()) {
            if (e.getProcessingFeeAmount() != null) total = total.add(e.getProcessingFeeAmount());
        }
        return total;
    }
}