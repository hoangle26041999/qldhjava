package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.ColorPalette;
import com.vietduc.plastic.service.ColorPaletteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý BẢNG MÀU (V01..V21) cho cửa nhựa.
 */
@Controller
@RequiredArgsConstructor
public class ColorPaletteController {

    private final ColorPaletteService service;

    @GetMapping("/admin/color-palette")
    public String list(Model model) {
        // Tự động seed 21 màu mặc định nếu collection rỗng
        service.seedDefaultsIfEmpty();
        model.addAttribute("items", service.getAll());
        return "admin/color-palette";
    }

    @PostMapping("/admin/color-palette/save")
    public String save(@ModelAttribute ColorPalette c, RedirectAttributes ra) {
        try {
            if (c.getId() == null || c.getId().isBlank()) {
                service.create(c);
                ra.addFlashAttribute("success", "Đã thêm màu " + c.getCode() + " — " + c.getName());
            } else {
                service.update(c.getId(), c);
                ra.addFlashAttribute("success", "Đã cập nhật màu " + c.getCode());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/color-palette";
    }

    @GetMapping("/admin/color-palette/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("success", "Đã xoá màu");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/color-palette";
    }

    @GetMapping("/admin/color-palette/reset")
    public String reset(RedirectAttributes ra) {
        try {
            service.getAll().forEach(c -> service.delete(c.getId()));
            service.seedDefaultsIfEmpty();
            ra.addFlashAttribute("success", "Đã reset bảng màu về 21 màu mặc định V01..V21");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/color-palette";
    }

    // ============ API JSON cho form tạo đơn ============
    @GetMapping("/api/color-palette")
    @ResponseBody
    public List<Map<String, Object>> all() {
        return service.getActive().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("code", c.getCode());
            m.put("name", c.getName());
            m.put("hexColor", c.getHexColor());
            m.put("groupName", c.getGroupName());
            return m;
        }).toList();
    }
}