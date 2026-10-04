package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.OrderStatusConfig;
import com.vietduc.plastic.service.OrderService;
import com.vietduc.plastic.service.OrderStatusConfigService;
import com.vietduc.plastic.util.RecentFeaturesTracker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Trang báo cáo thống kê tổng quan.
 * Đếm đơn theo trạng thái (lấy từ bảng order_statuses), tổng doanh thu, v.v.
 */
@Controller
@RequiredArgsConstructor
public class ReportsController {

    private final OrderService orderService;
    private final OrderStatusConfigService orderStatusConfigService;

    @GetMapping("/reports")
    public String reports(Model model, HttpSession session) {
        RecentFeaturesTracker.touch(session, RecentFeaturesTracker.FEATURE_REPORT);
        long total = orderService.getAllOrders().size();
        Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        for (OrderStatusConfig cfg : orderStatusConfigService.getAll()) {
            long cnt = orderService.countOrdersByStatus(cfg.getCode());
            byStatus.put(cfg.getName(), cnt);
        }
        BigDecimal revenue = orderService.calculateTotalRevenue();

        // Biến phụ trợ cho template (count theo từng trạng thái)
        long count_cho_xac_nhan = orderService.countOrdersByStatus("CHO_XAC_NHAN");
        long count_da_xac_nhan = orderService.countOrdersByStatus("DA_XAC_NHAN");
        long count_dang_san_xuat = orderService.countOrdersByStatus("DANG_SAN_XUAT");
        long count_hoan_thanh = orderService.countOrdersByStatus("HOAN_THANH");
        long count_da_huy = orderService.countOrdersByStatus("DA_HUY");

        model.addAttribute("totalOrders", total);
        model.addAttribute("byStatus", byStatus);
        model.addAttribute("revenue", revenue);
        model.addAttribute("count_cho_xac_nhan", count_cho_xac_nhan);
        model.addAttribute("count_da_xac_nhan", count_da_xac_nhan);
        model.addAttribute("count_dang_san_xuat", count_dang_san_xuat);
        model.addAttribute("count_hoan_thanh", count_hoan_thanh);
        model.addAttribute("count_da_huy", count_da_huy);
        return "reports/index";
    }
}
