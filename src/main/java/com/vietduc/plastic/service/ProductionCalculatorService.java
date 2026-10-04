package com.vietduc.plastic.service;

import com.vietduc.plastic.model.*;
import org.springframework.stereotype.Service;

/**
 * Bộ tính toán thông số kỹ thuật cho đơn hàng cửa nhựa.
 *
 * Quy tắc lấy từ ghi chép kỹ thuật của nhà máy Việt Đức (theo bảng ảnh KTO):
 *
 * 1) KÍCH THƯỚC CÁNH SẢN XUẤT (áp dụng cho cả Hệ 40 và Hệ 45)
 *    - R cánh SX = R ô chờ - 80
 *    - C cánh SX = C ô chờ - 45
 *
 * 2) KHUÔN ĐỨNG (chọn theo dày tường D)
 *    Hệ 40:
 *      D < 100         → 80
 *      100 ≤ D < 125   → 100
 *      125 ≤ D < 170   → 125
 *      170 ≤ D < 220   → 170
 *      D ≥ 220         → 220
 *    Hệ 45:
 *      D < 125         → 100
 *      125 ≤ D < 170   → 125
 *      170 ≤ D < 220   → 170
 *      D ≥ 220         → 220
 *
 * 3) KHUÔN NGANG (theo R ô chờ)
 *      R ≤ 900         → 1800
 *      900 < R < 1100  → 2200
 *      R ≥ 1100        → Tùy chỉnh (cần KTO duyệt)
 *
 * 4) KHUÔN CAO (theo C ô chờ)
 *      C ≤ 2200        → 2200
 *      2200 < C ≤ 2250 → 2250
 *      2250 < C ≤ 2400 → 2400
 *      2400 < C ≤ 2450 → 2450
 *      2450 < C ≤ 2700 → 2700
 *      C > 2700        → Tùy chỉnh
 *
 * 5) NẸP ĐỨNG (theo C cánh SX) — bộ × 4 thanh
 *      C SX ≤ 2250     → 2250 + 50 (cộng 50mm đầu nối)
 *      2250 < C ≤ 2450 → 2450 + 50
 *      2450 < C ≤ 2550 → 2550 + 50
 *      2550 < C ≤ 2750 → 2750 + 50
 *      C SX > 2750     → Tùy chỉnh
 *
 * 6) NẸP NGANG (theo R ô chờ)
 *      R ≥ 950         → 2100
 *      R < 950         → không có
 *
 * 7) NẸP CHỐNG HỞ (bộ gió / cửa đôi): 1 bộ = 2 thanh
 */
@Service
public class ProductionCalculatorService {

    /**
     * Tính toán toàn bộ thông số kỹ thuật từ ô chờ + dày tường + hệ + loại cửa.
     */
    public ProductionSpec calculate(Integer openingWidth, Integer openingHeight,
                                     Integer wallThickness, Integer quantity,
                                     DoorSystem doorSystem, DoorType doorType) {
        ProductionSpec spec = new ProductionSpec();
        spec.setOpeningWidth(openingWidth);
        spec.setOpeningHeight(openingHeight);
        spec.setWallThickness(wallThickness);
        spec.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
        spec.setNeedsReview(false);

        if (openingWidth == null || openingHeight == null || wallThickness == null) {
            // Chưa đủ dữ liệu để tính
            return spec;
        }

        // ============ Kích thước cánh sản xuất ============
        // Hệ 40 và Hệ 45 dùng chung công thức trừ hao:
        //   cánh R SX = R ô chờ - 80
        //   cánh C SX = C ô chờ - 45
        int leafW = openingWidth - 80;
        int leafH = openingHeight - 45;
        spec.setLeafWidth(leafW);
        spec.setLeafHeight(leafH);

        // ============ Khuôn đứng (theo dày tường + hệ) ============
        FrameChoice vf = chooseVerticalFrame(wallThickness, doorSystem);
        spec.setVerticalFrame(vf.value);
        spec.setVerticalFrameCustom(vf.custom);
        if (vf.custom) spec.setNeedsReview(true);

        // ============ Khuôn ngang (theo R ô chờ) ============
        FrameChoice hf = chooseHorizontalFrame(openingWidth);
        spec.setHorizontalFrame(hf.value);
        spec.setHorizontalFrameCustom(hf.custom);
        if (hf.custom) spec.setNeedsReview(true);

        // ============ Khuôn cao (theo C ô chờ) ============
        FrameChoice hgf = chooseHeightFrame(openingHeight);
        spec.setHeightFrame(hgf.value);
        spec.setHeightFrameCustom(hgf.custom);
        if (hgf.custom) spec.setNeedsReview(true);

        // ============ Nẹp đứng (theo chiều cao cánh SX) ============
        TrimChoice vt = chooseVerticalTrim(leafH);
        spec.setVerticalTrim(vt.value);
        spec.setVerticalTrimCustom(vt.custom);
        if (vt.custom) spec.setNeedsReview(true);
        // Số lượng = bộ × 4
        spec.setVerticalTrimQuantity(spec.safeQuantity() * 4);

        // ============ Nẹp ngang (theo R ô chờ) ============
        TrimChoice ht = chooseHorizontalTrim(openingWidth);
        spec.setHorizontalTrim(ht.value);
        spec.setHorizontalTrimCustom(ht.custom);
        if (ht.value == null) spec.setHorizontalTrimCustom(false);

        // ============ Nẹp chống hở (bộ gió / cửa đôi / cửa đặc biệt) ============
        int boGio = (doorType == DoorType.CUA_BON_GIO) ? 1 : 0;
        int cuaDoi = (doorType == DoorType.CUA_DOI) ? 1 : 0;
        if (boGio + cuaDoi > 0) {
            spec.setAntiGapTrimQuantity(spec.safeQuantity() * 2);
        }

        return spec;
    }

    /**
     * Chọn khuôn đứng theo dày tường D và hệ.
     */
    public FrameChoice chooseVerticalFrame(int D, DoorSystem system) {
        if (system == DoorSystem.HE_40) {
            if (D <= 80)  return new FrameChoice(80, true);     // TODO cần xác nhận (ghi chú 80 < D < 100)
            if (D < 100)  return new FrameChoice(80, false);
            if (D < 125)  return new FrameChoice(100, false);
            if (D < 170)  return new FrameChoice(125, false);
            if (D < 220)  return new FrameChoice(170, false);
            return new FrameChoice(220, false);
        } else { // HE_45
            if (D < 125)  return new FrameChoice(100, false);
            if (D < 170)  return new FrameChoice(125, false);   // Hệ 45: 125 ≤ D < 170 → 125
            if (D < 220)  return new FrameChoice(170, false);
            return new FrameChoice(220, false);
        }
    }

    /**
     * Khuôn ngang theo R ô chờ.
     */
    public FrameChoice chooseHorizontalFrame(int R) {
        if (R <= 900) return new FrameChoice(1800, false);
        if (R < 1100) return new FrameChoice(2200, false);
        return new FrameChoice(0, true); // tùy chỉnh
    }

    /**
     * Khuôn cao theo C ô chờ.
     */
    public FrameChoice chooseHeightFrame(int C) {
        if (C <= 2200) return new FrameChoice(2200, false);
        if (C <= 2250) return new FrameChoice(2250, false);
        if (C <= 2400) return new FrameChoice(2400, false);
        if (C <= 2450) return new FrameChoice(2450, false);
        if (C <= 2700) return new FrameChoice(2700, false);
        return new FrameChoice(0, true); // tùy chỉnh
    }

    /**
     * Nẹp đứng theo chiều cao cánh SX (mm).
     * value là chiều dài (mm); custom=true nếu vượt bảng.
     */
    public TrimChoice chooseVerticalTrim(int leafHeight) {
        if (leafHeight <= 2250) return new TrimChoice(2250 + 50, false);
        if (leafHeight <= 2450) return new TrimChoice(2450 + 50, false);
        if (leafHeight <= 2550) return new TrimChoice(2550 + 50, false);
        if (leafHeight <= 2750) return new TrimChoice(2750 + 50, false);
        return new TrimChoice(0, true); // tùy chỉnh
    }

    /**
     * Nẹp ngang theo R ô chờ.
     */
    public TrimChoice chooseHorizontalTrim(int R) {
        if (R >= 950) return new TrimChoice(2100, false);
        return new TrimChoice(null, false); // không có nẹp ngang
    }

    // ============ Helper inner classes ============
    public record FrameChoice(int value, boolean custom) {}
    public record TrimChoice(Integer value, boolean custom) {}
}