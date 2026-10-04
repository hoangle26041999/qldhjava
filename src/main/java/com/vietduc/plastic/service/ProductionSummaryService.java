package com.vietduc.plastic.service;

import com.vietduc.plastic.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tổng hợp nhiều đơn hàng thành bảng sản xuất cho nhà máy.
 *
 *  - Tổng số cánh
 *  - Tổng khuôn đứng
 *  - Tổng khuôn ngang
 *  - Tổng khuôn cao
 *  - Tổng nẹp đứng
 *  - Tổng nẹp ngang
 *  - Tổng phụ kiện (khóa, bản lề, ô kính)
 *  - Phân loại theo hệ 40 / 45
 *  - Phân loại theo loại cửa
 *  - Phân loại theo mẫu
 *  - Phân loại theo màu
 */
@Service
@RequiredArgsConstructor
public class ProductionSummaryService {

    /**
     * Tạo báo cáo tổng hợp từ danh sách đơn.
     */
    public SummaryReport summarize(List<Order> orders) {
        SummaryReport report = new SummaryReport();

        // Lọc đơn có trạng thái đang xử lý / chờ SX (bỏ các status = "DA_HUY" hoặc "HOAN_THANH")
        List<Order> productionOrders = orders.stream()
                .filter(o -> !"DA_HUY".equals(o.getStatus()) && !"HOAN_THANH".equals(o.getStatus()))
                .toList();

        report.setOrderCount(productionOrders.size());

        // ===== Tổng số cánh =====
        int totalLeaves = productionOrders.stream()
                .filter(o -> o.getQuantity() != null)
                .mapToInt(o -> o.getQuantity())
                .sum();
        report.setTotalLeaves(totalLeaves);

        // ===== Tổng khuôn đứng =====
        Map<Integer, Integer> verticalFrameQty = new TreeMap<>();
        for (Order o : productionOrders) {
            if (o.getVerticalFrame() != null && o.getQuantity() != null) {
                // mỗi đơn cần verticalFrame cho mỗi bộ (giả định 1 bộ = 2 thanh khuôn đứng)
                int qty = o.getQuantity() * 2;
                verticalFrameQty.merge(o.getVerticalFrame(), qty, Integer::sum);
            }
        }
        report.setVerticalFrameQty(verticalFrameQty);

        // ===== Tổng khuôn ngang =====
        Map<Integer, Integer> horizontalFrameQty = new TreeMap<>();
        for (Order o : productionOrders) {
            if (o.getHorizontalFrame() != null && o.getQuantity() != null) {
                int qty = o.getQuantity();
                horizontalFrameQty.merge(o.getHorizontalFrame(), qty, Integer::sum);
            }
        }
        report.setHorizontalFrameQty(horizontalFrameQty);

        // ===== Tổng khuôn cao =====
        Map<Integer, Integer> heightFrameQty = new TreeMap<>();
        for (Order o : productionOrders) {
            if (o.getHeightFrame() != null && o.getQuantity() != null) {
                int qty = o.getQuantity();
                heightFrameQty.merge(o.getHeightFrame(), qty, Integer::sum);
            }
        }
        report.setHeightFrameQty(heightFrameQty);

        // ===== Tổng nẹp đứng =====
        Map<Integer, Integer> verticalTrimQty = new TreeMap<>();
        for (Order o : productionOrders) {
            if (o.getVerticalTrim() != null && o.getVerticalTrimQuantity() != null) {
                verticalTrimQty.merge(o.getVerticalTrim(), o.getVerticalTrimQuantity(), Integer::sum);
            }
        }
        report.setVerticalTrimQty(verticalTrimQty);

        // ===== Tổng nẹp ngang =====
        Map<Integer, Integer> horizontalTrimQty = new TreeMap<>();
        for (Order o : productionOrders) {
            if (o.getHorizontalTrim() != null && o.getQuantity() != null) {
                int qty = o.getQuantity();
                horizontalTrimQty.merge(o.getHorizontalTrim(), qty, Integer::sum);
            }
        }
        report.setHorizontalTrimQty(horizontalTrimQty);

        // ===== Phân loại theo hệ =====
        Map<String, Long> bySystem = productionOrders.stream()
                .filter(o -> o.getDoorSystem() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getDoorSystem().getDisplayName(),
                        Collectors.counting()
                ));
        report.setCountBySystem(bySystem);

        // ===== Phân loại theo loại cửa =====
        Map<String, Long> byDoorType = productionOrders.stream()
                .filter(o -> o.getDoorType() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getDoorType().getDisplayName(),
                        Collectors.counting()
                ));
        report.setCountByDoorType(byDoorType);

        // ===== Phân loại theo mẫu =====
        Map<String, Long> byPattern = productionOrders.stream()
                .filter(o -> o.getPattern() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getPattern().getDisplayName(),
                        Collectors.counting()
                ));
        report.setCountByPattern(byPattern);

        // ===== Phân loại theo màu =====
        Map<String, Long> byColor = productionOrders.stream()
                .filter(o -> o.getColorName() != null && !o.getColorName().isBlank())
                .collect(Collectors.groupingBy(
                        o -> o.getColorName().trim(),
                        Collectors.counting()
                ));
        report.setCountByColor(byColor);

        // ===== Tổng giá bán =====
        BigDecimal totalAmount = productionOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalAmount(totalAmount);

        return report;
    }

    // ============ Inner DTO ============
    @lombok.Data
    public static class SummaryReport {
        private int orderCount;
        private int totalLeaves;
        private Map<Integer, Integer> verticalFrameQty;
        private Map<Integer, Integer> horizontalFrameQty;
        private Map<Integer, Integer> heightFrameQty;
        private Map<Integer, Integer> verticalTrimQty;
        private Map<Integer, Integer> horizontalTrimQty;
        private Map<String, Long> countBySystem;
        private Map<String, Long> countByDoorType;
        private Map<String, Long> countByPattern;
        private Map<String, Long> countByColor;
        private BigDecimal totalAmount;
    }
}