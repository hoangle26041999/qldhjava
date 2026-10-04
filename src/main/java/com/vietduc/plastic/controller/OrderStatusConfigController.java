package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.OrderStatusConfig;
import com.vietduc.plastic.service.OrderStatusConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Quản lý bảng Danh mục trạng thái đơn hàng.
 */
@Controller
@RequiredArgsConstructor
public class OrderStatusConfigController {

    private final OrderStatusConfigService service;

    @GetMapping("/admin/order-statuses")
    public String list(Model model) {
        model.addAttribute("items", service.getAll());
        return "admin/order-statuses";
    }

    @PostMapping("/admin/order-statuses/save")
    public String save(@ModelAttribute OrderStatusConfig cfg, RedirectAttributes ra) {
        try {
            if (cfg.getId() == null || cfg.getId().isBlank()) {
                service.create(cfg);
                ra.addFlashAttribute("success", "Đã thêm trạng thái " + cfg.getName());
            } else {
                service.update(cfg.getId(), cfg);
                ra.addFlashAttribute("success", "Đã cập nhật trạng thái " + cfg.getName());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/order-statuses";
    }

    @GetMapping("/admin/order-statuses/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("success", "Đã xoá trạng thái");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/order-statuses";
    }
}
