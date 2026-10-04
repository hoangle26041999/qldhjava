package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.ProcessingFee;
import com.vietduc.plastic.service.ProcessingFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý bảng Phát sinh gia công (CNC, GHÉP CÁNH, CHỈ SOI, PHÀO NỔI, Ô FIX, KHÁC, ...).
 *
 * CRUD hoàn chỉnh:
 *  - GET    /admin/processing-fees              -> trang danh sách
 *  - POST   /admin/processing-fees/save         -> thêm mới hoặc cập nhật
 *  - GET    /admin/processing-fees/delete/{id}  -> xoá
 *  - GET    /api/processing-fees                -> JSON cho form tạo đơn (chỉ lấy active=true)
 */
@Controller
@RequiredArgsConstructor
public class ProcessingFeeController {

    private final ProcessingFeeService service;

    @GetMapping("/admin/processing-fees")
    public String list(Model model) {
        model.addAttribute("items", service.getAll());
        return "admin/processing-fees";
    }

    /**
     * Lưu (thêm mới hoặc cập nhật). Phân biệt nhờ field id:
     *  - id rỗng / null -> CREATE
     *  - id có giá trị  -> UPDATE
     */
    @PostMapping("/admin/processing-fees/save")
    public String save(@ModelAttribute("fee") ProcessingFee fee,
                       @RequestParam(value = "active", required = false) String activeFlag,
                       @RequestParam(value = "displayOrder", required = false) String displayOrderRaw,
                       RedirectAttributes ra) {
        try {
            // Checkbox HTML: gửi "true" nếu tick, không gửi gì nếu bỏ tick.
            fee.setActive("true".equals(activeFlag));

            // Nếu id rỗng/null -> CREATE. Nếu có id -> UPDATE.
            // FIX: set null nếu rỗng để MongoDB tự generate _id mới.
            if (fee.getId() == null || fee.getId().isBlank()) {
                fee.setId(null);
                ProcessingFee saved = service.create(fee);
                ra.addFlashAttribute("success", "Đã thêm phát sinh \"" + saved.getName() + "\"");
            } else {
                // Nếu user chỉnh thứ tự trong form -> cập nhật displayOrder.
                if (displayOrderRaw != null && !displayOrderRaw.isBlank()) {
                    try {
                        fee.setDisplayOrder(Integer.parseInt(displayOrderRaw.trim()));
                    } catch (NumberFormatException ignore) { /* để null, service giữ nguyên */ }
                }
                ProcessingFee saved = service.update(fee.getId(), fee);
                ra.addFlashAttribute("success", "Đã cập nhật phát sinh \"" + saved.getName() + "\"");
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi không xác định: " + e.getMessage());
        }
        return "redirect:/admin/processing-fees";
    }

    @GetMapping("/admin/processing-fees/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("success", "Đã xoá phát sinh");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi xoá: " + e.getMessage());
        }
        return "redirect:/admin/processing-fees";
    }

    /**
     * Dọn rác: xoá các document có id rỗng/null (do test thủ công tạo ra).
     * GET cho dễ gọi từ trình duyệt khi cần.
     */
    @GetMapping("/admin/processing-fees/cleanup")
    public String cleanup(RedirectAttributes ra) {
        int removed = service.deleteEmptyIds();
        ra.addFlashAttribute(removed > 0 ? "success" : "error",
                removed > 0 ? ("Đã dọn " + removed + " phát sinh lỗi (id rỗng).")
                             : "Không có phát sinh lỗi cần dọn.");
        return "redirect:/admin/processing-fees";
    }

    // ============ API: cho form tạo đơn ============
    @GetMapping("/api/processing-fees")
    @ResponseBody
    public List<Map<String, Object>> all() {
        return service.getActive().stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("code", f.getCode());
            m.put("name", f.getName());
            m.put("defaultUnitPrice", f.getDefaultUnitPrice());
            m.put("unit", f.getUnit());
            return m;
        }).toList();
    }
}