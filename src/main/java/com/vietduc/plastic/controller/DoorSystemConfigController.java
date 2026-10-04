package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.DoorSystemConfig;
import com.vietduc.plastic.service.DoorSystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Quản lý bảng Dòng cánh (Hệ 40, Hệ 45, ...).
 */
@Controller
@RequiredArgsConstructor
public class DoorSystemConfigController {

    private final DoorSystemConfigService service;

    @GetMapping("/admin/door-systems")
    public String list(Model model) {
        model.addAttribute("items", service.getAll());
        return "admin/door-systems";
    }

    @PostMapping("/admin/door-systems/save")
    public String save(@ModelAttribute DoorSystemConfig cfg, RedirectAttributes ra) {
        try {
            if (cfg.getId() == null || cfg.getId().isBlank()) {
                service.create(cfg);
                ra.addFlashAttribute("success", "Đã thêm hệ cửa " + cfg.getName());
            } else {
                service.update(cfg.getId(), cfg);
                ra.addFlashAttribute("success", "Đã cập nhật hệ cửa " + cfg.getName());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/door-systems";
    }

    @GetMapping("/admin/door-systems/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("success", "Đã xoá hệ cửa");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/door-systems";
    }

    // ============ API: cho form tạo đơn ============
    @GetMapping("/api/door-systems")
    @ResponseBody
    public List<Map<String, Object>> all() {
        return service.getActive().stream().map(c -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("isDefault", c.getIsDefault());
            return m;
        }).toList();
    }

    @GetMapping("/api/door-systems/default")
    @ResponseBody
    public ResponseEntity<?> getDefault() {
        DoorSystemConfig def = service.getDefault();
        if (def == null) return ResponseEntity.ok(Map.of("notFound", true));
        return ResponseEntity.ok(def);
    }
}
