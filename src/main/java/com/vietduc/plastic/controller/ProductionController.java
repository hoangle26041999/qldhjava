package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.Order;
import com.vietduc.plastic.model.OrderStatus;
import com.vietduc.plastic.service.OrderService;
import com.vietduc.plastic.service.ProductionSummaryService;
import com.vietduc.plastic.service.ProductionSummaryService.SummaryReport;
import com.vietduc.plastic.util.RecentFeaturesTracker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductionController {

    private final OrderService orderService;
    private final ProductionSummaryService summaryService;

    /**
     * Trang tổng hợp sản xuất - cho phép chọn nhiều đơn để cộng dồn vật tư.
     */
    @GetMapping("/production/summary")
    public String productionSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String ids,
            Model model, HttpSession session) {
        RecentFeaturesTracker.touch(session, RecentFeaturesTracker.FEATURE_PRODUCTION);

        List<Order> selectedOrders = new ArrayList<>();

        // 1) Nếu có ids (checkbox chọn) → lấy các đơn được chọn
        if (ids != null && !ids.isBlank()) {
            for (String id : ids.split(",")) {
                orderService.getOrderById(id.trim()).ifPresent(selectedOrders::add);
            }
        } else if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            // 2) Nếu có khoảng ngày → lấy tất cả trong khoảng
            LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
            LocalDateTime toDt = LocalDate.parse(to).atTime(23, 59, 59);
            selectedOrders = orderService.getOrdersByDateRange(fromDt, toDt);
        } else {
            // 3) Mặc định: tất cả đơn chưa hoàn thành / chưa hủy
            selectedOrders = orderService.getAllOrders().stream()
                    .filter(o -> !"DA_HUY".equals(o.getStatus()) && !"HOAN_THANH".equals(o.getStatus()))
                    .toList();
        }

        SummaryReport report = summaryService.summarize(selectedOrders);

        model.addAttribute("orders", selectedOrders);
        model.addAttribute("report", report);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("allOrders", orderService.getAllOrders());
        model.addAttribute("statuses", OrderStatus.values());

        return "production/summary";
    }
}