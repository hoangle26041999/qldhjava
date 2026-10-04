package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.Order;
import com.vietduc.plastic.service.OrderService;
import com.vietduc.plastic.service.ProductionOrderService;
import com.vietduc.plastic.service.ProductionOrderService.ProductionOrderReport;
import com.vietduc.plastic.util.RecentFeaturesTracker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller cho tính năng LỆNH SẢN XUẤT THEO NHÓM.
 * <p>
 * - GET /production/orders          : Bảng lệnh SX (xem + chọn đơn)
 * - GET /production/orders/print    : Bảng in lệnh SX
 * - GET /production/material-summary: Bảng tổng hợp vật tư
 */
@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionOrderController {

    private final OrderService orderService;
    private final ProductionOrderService productionOrderService;

    /**
     * Trang lệnh sản xuất - gom các bộ cửa từ nhiều đơn thành nhóm theo quy cách.
     */
    @GetMapping("/orders")
    public String productionOrders(
            @RequestParam(required = false) String ids,
            Model model, HttpSession session) {
        RecentFeaturesTracker.touch(session, RecentFeaturesTracker.FEATURE_PRODUCTION);

        List<Order> selectedOrders = resolveOrders(ids);
        ProductionOrderReport report = productionOrderService.buildReport(selectedOrders);

        model.addAttribute("report", report);
        model.addAttribute("selectedIds", ids);
        model.addAttribute("allOrders", orderService.getAllOrders());
        model.addAttribute("selectedOrders", selectedOrders);
        return "production/orders";
    }

    /**
     * Bảng in lệnh sản xuất (print-friendly).
     */
    @GetMapping("/orders/print")
    public String printProductionOrders(
            @RequestParam(required = false) String ids,
            Model model, HttpSession session) {
        List<Order> selectedOrders = resolveOrders(ids);
        ProductionOrderReport report = productionOrderService.buildReport(selectedOrders);

        model.addAttribute("report", report);
        model.addAttribute("selectedIds", ids);
        return "production/orders-print";
    }

    /**
     * Bảng tổng hợp vật tư từ các nhóm SX (ảnh 2).
     */
    @GetMapping("/material-summary")
    public String materialSummary(
            @RequestParam(required = false) String ids,
            Model model, HttpSession session) {
        RecentFeaturesTracker.touch(session, RecentFeaturesTracker.FEATURE_PRODUCTION);

        List<Order> selectedOrders = resolveOrders(ids);
        ProductionOrderReport report = productionOrderService.buildReport(selectedOrders);

        model.addAttribute("report", report);
        model.addAttribute("selectedIds", ids);
        model.addAttribute("allOrders", orderService.getAllOrders());
        model.addAttribute("selectedOrders", selectedOrders);
        return "production/material-summary";
    }

    /**
     * Chọn đơn theo ids (nếu có) hoặc mặc định: tất cả đơn chưa hoàn thành / chưa hủy.
     */
    private List<Order> resolveOrders(String ids) {
        if (ids != null && !ids.isBlank()) {
            List<Order> picked = new ArrayList<>();
            for (String id : ids.split(",")) {
                orderService.getOrderById(id.trim()).ifPresent(picked::add);
            }
            if (!picked.isEmpty()) return picked;
        }
        // Mặc định: tất cả đơn chưa hoàn thành / chưa hủy
        return orderService.getAllOrders().stream()
                .filter(o -> !"DA_HUY".equals(o.getStatus()) && !"HOAN_THANH".equals(o.getStatus()))
                .toList();
    }
}