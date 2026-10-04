package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.Customer;
import com.vietduc.plastic.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý Khách hàng — UI + REST API cho autocomplete.
 */
@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ============ Trang quản trị ============
    @GetMapping("/admin/customers")
    public String list(@RequestParam(required = false) String search, Model model) {
        List<Customer> customers = customerService.search(search);
        model.addAttribute("customers", customers);
        model.addAttribute("search", search);
        return "admin/customers";
    }

    @PostMapping("/admin/customers/save")
    public String save(@ModelAttribute Customer customer, RedirectAttributes ra) {
        try {
            if (customer.getId() == null || customer.getId().isBlank()) {
                customerService.create(customer);
            } else {
                customerService.update(customer.getId(), customer);
            }
            ra.addFlashAttribute("success", "Đã lưu khách hàng " + customer.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @GetMapping("/admin/customers/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            customerService.delete(id);
            ra.addFlashAttribute("success", "Đã xoá khách hàng");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ============ REST API: cho form tạo đơn ============

    /** Tìm KH theo SĐT (dùng để auto-fill form). */
    @GetMapping("/api/customers/phone/{phone}")
    @ResponseBody
    public ResponseEntity<?> getByPhone(@PathVariable String phone) {
        return customerService.getByPhone(phone)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("notFound", true)));
    }

    /** Tìm kiếm KH (autocomplete). */
    @GetMapping("/api/customers/search")
    @ResponseBody
    public List<Map<String, Object>> search(@RequestParam String keyword) {
        return customerService.search(keyword).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("customerCode", c.getCustomerCode());
            m.put("name", c.getName());
            m.put("phone", c.getPhone());
            m.put("address", c.getAddress());
            return m;
        }).toList();
    }

    /** Lưu nhanh khách hàng từ form tạo đơn (upsert theo SĐT). */
    @PostMapping(value = "/api/customers/quick-save", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> quickSave(@RequestBody Customer c) {
        try {
            Customer saved = customerService.findOrCreate(c.getName(), c.getPhone(), c.getAddress(), c.getNotes());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
