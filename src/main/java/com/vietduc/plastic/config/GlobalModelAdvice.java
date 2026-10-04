package com.vietduc.plastic.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Tự động thêm thuộc tính 'active' vào model cho layout, dựa trên URI hiện tại.
 * Tránh phải sửa từng controller khi refactor sang layout chung.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("active")
    public String active(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String path = uri != null ? uri : "";
        // Thứ tự quan trọng: kiểm tra các path cụ thể TRƯỚC các path tổng quát
        if (path.equals("/") || path.equals("")) return "dashboard";
        if (path.startsWith("/orders/status")) return "orders-status";
        if (path.startsWith("/orders/new-multi")) return "order-multi";
        if (path.startsWith("/orders/new")) return "order-new";
        if (path.startsWith("/orders")) return "orders";
        if (path.startsWith("/reports")) return "reports";
        if (path.startsWith("/production/orders")) return "production-orders";
        if (path.startsWith("/production/material-summary")) return "production-material";
        if (path.startsWith("/production")) return "production";
        if (path.startsWith("/admin/customers")) return "customers";
        if (path.startsWith("/admin/door-systems")) return "door-systems";
        if (path.startsWith("/admin/processing-fees")) return "fees";
        if (path.startsWith("/admin/color-palette")) return "colors";
        if (path.startsWith("/admin/order-statuses")) return "order-statuses";
        if (path.startsWith("/admin/print-pattern")) return "print-pattern";
        return "";
    }
}