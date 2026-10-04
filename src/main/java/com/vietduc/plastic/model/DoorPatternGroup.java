package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Một MẪU cửa trong đơn hàng.
 * <p>
 * Mỗi đơn có thể gồm nhiều mẫu cửa khác nhau
 * (vd: mẫu Trơn+CNC hệ 40, mẫu WC hệ 40, mẫu Cửa chính CNC+Phào nổi).
 * Mỗi mẫu có thể có nhiều BỘ cửa cụ thể (cùng thông số nhưng khác vị trí).
 * <p>
 * Hệ thống tự động tính ProductionSpec riêng cho từng bộ (DoorItem)
 * dựa trên thông số của mẫu (doorSystem/doorType/...) và kích thước ô chờ của bộ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoorPatternGroup {

    /**
     * Mã nhóm (VD-DH001-MAU01). Sinh tự động khi tạo.
     */
    private String groupCode;

    /**
     * Tên mẫu hiển thị cho KTO (mặc định sinh từ doorType + patterns, vd "Cửa phòng - Trơn + CNC").
     */
    private String name;

    // ============ Thông số kỹ thuật CHUNG cho cả mẫu ============
    private DoorSystem doorSystem;        // Hệ 40 / 45
    private DoorType doorType;            // Cửa phòng / chính / WC
    private List<DoorPattern> patterns = new ArrayList<>();   // Trơn + CNC + Phào + Chỉ nhôm
    private DoorPattern pattern;          // legacy single (backward-compat)
    private String colorName;             // Màu
    private GlassType glassType;          // Ô kính

    // ============ Phụ phí (cờ cho cả mẫu) ============
    private Boolean extraScrew;
    private Boolean extraCnc;
    private Boolean extraGlassShort;
    private Boolean extraGlassLong;
    private Boolean extraVent;
    private Boolean extraAluminum;
    private Boolean extraMolding;
    private Boolean extraPaintDoor;
    private Boolean extraBo;
    private Boolean extraMoldingGia;
    private Boolean extraLock;
    private Boolean extraColor;

    /**
     * Danh sách các BỘ cửa cụ thể của mẫu này
     * (cùng thông số nhưng khác vị trí, khác kích thước, khác khóa).
     */
    private List<DoorItem> doors = new ArrayList<>();

    // ============ Helpers ============

    public List<DoorItem> getDoorsSafe() {
        if (doors == null) doors = new ArrayList<>();
        return doors;
    }

    public List<DoorPattern> getPatternsSafe() {
        if (patterns == null) patterns = new ArrayList<>();
        return patterns;
    }

    /**
     * Tạo mã nhóm tự động.
     */
    public static String generateGroupCode(String orderCode, int index) {
        return orderCode + "-MAU" + String.format("%02d", index);
    }

    /**
     * Tự động sinh tên mẫu nếu chưa có (vd "Cửa phòng — Trơn + CNC").
     */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) return name;
        StringBuilder sb = new StringBuilder();
        if (doorType != null) sb.append(doorType.getDisplayName());
        if (patterns != null && !patterns.isEmpty()) {
            sb.append(" — ").append(String.join(" + ",
                    patterns.stream().map(DoorPattern::getDisplayName).toList()));
        } else if (pattern != null) {
            sb.append(" — ").append(pattern.getDisplayName());
        }
        if (doorSystem != null) sb.append(" (").append(doorSystem.getDisplayName()).append(")");
        return sb.length() == 0 ? "Mẫu cửa" : sb.toString();
    }

    /**
     * Suy ra các phụ phí (extra*) từ patterns và glassType.
     */
    public void inferExtraFeesFromPatterns() {
        if (patterns == null) patterns = new ArrayList<>();
        if (patterns.contains(DoorPattern.CNC) && extraCnc == null) extraCnc = true;
        if (patterns.contains(DoorPattern.PHAO_NOI) && extraMolding == null) extraMolding = true;
        if (patterns.contains(DoorPattern.CHI_NHOM) && extraAluminum == null) extraAluminum = true;
    }
}