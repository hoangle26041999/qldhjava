package com.vietduc.plastic.controller;

import com.vietduc.plastic.model.*;
import com.vietduc.plastic.service.ColorPaletteService;
import com.vietduc.plastic.service.DoorSystemConfigService;
import com.vietduc.plastic.service.OrderService;
import com.vietduc.plastic.service.OrderStatusConfigService;
import com.vietduc.plastic.service.ProcessingFeeService;
import com.vietduc.plastic.service.ProductionCalculatorService;
import com.vietduc.plastic.util.RecentFeaturesTracker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ProductionCalculatorService calculator;
    private final DoorSystemConfigService doorSystemConfigService;
    private final ProcessingFeeService processingFeeService;
    private final OrderStatusConfigService orderStatusConfigService;
    private final ColorPaletteService colorPaletteService;

        // ============ Trang chủ ============
    @GetMapping("/")
    public String dashboard(Model model, HttpSession session) {
        List<Order> allOrders = orderService.getAllOrders();

        long totalOrders = allOrders.size();
        long pendingOrders = orderService.countOrdersByStatus(OrderStatus.CHO_XAC_NHAN.name());
        long processingOrders = orderService.countOrdersByStatus(OrderStatus.DANG_SAN_XUAT.name());
        long completedOrders = orderService.countOrdersByStatus(OrderStatus.HOAN_THANH.name());

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("processingOrders", processingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("recentOrders", allOrders.stream().limit(5).toList());
        // Map statusCode → config (dùng cho hiển thị tên + màu trên dashboard)
        java.util.Map<String, OrderStatusConfig> statusConfigs = new java.util.HashMap<>();
        for (OrderStatusConfig c : orderStatusConfigService.getAll()) {
            statusConfigs.put(c.getCode(), c);
        }
        model.addAttribute("statusConfigs", statusConfigs);
        // Tính năng vừa truy cập (sắp xếp thời gian gần nhất)
        model.addAttribute("recentFeatures", RecentFeaturesTracker.getSorted(session));

        return "dashboard";
    }

    // ============ Danh sách đơn hàng ============
    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            Model model) {

        // Lấy dữ liệu lọc theo các tiêu chí
        List<Order> orders;
        boolean hasFilter = (search != null && !search.isBlank())
                || status != null
                || fromDate != null
                || toDate != null;

        if (!hasFilter) {
            // Mặc định: hiển thị đơn gần nhất (sort desc đã có trong service)
            orders = orderService.getAllOrders();
        } else {
            // Lọc theo tháng có fromDate/toDate (ưu tiên), nếu không thì dùng search/status
            if (fromDate != null || toDate != null) {
                java.time.LocalDateTime start = fromDate != null
                        ? fromDate.atStartOfDay()
                        : java.time.LocalDateTime.of(1970, 1, 1, 0, 0);
                java.time.LocalDateTime end = toDate != null
                        ? toDate.atTime(23, 59, 59)
                        : java.time.LocalDateTime.of(9999, 12, 31, 23, 59);
                orders = orderService.getOrdersByDateRange(start, end);
                // Lọc thêm theo status/search nếu có
                if (status != null && !status.isBlank()) {
                    orders = orders.stream().filter(o -> status.equals(o.getStatus())).toList();
                }
                if (search != null && !search.isBlank()) {
                    String kw = search.toLowerCase();
                    orders = orders.stream().filter(o ->
                            (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(kw))
                            || (o.getCustomerPhone() != null && o.getCustomerPhone().contains(kw))
                            || (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(kw))
                    ).toList();
                }
            } else if (search != null && !search.isEmpty()) {
                orders = orderService.searchOrders(search);
            } else if (status != null) {
                orders = orderService.getOrdersByStatus(status);
            } else {
                orders = orderService.getAllOrders();
            }
        }

        model.addAttribute("orders", orders);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", OrderStatus.values());
        // Dropdown lấy từ bảng config (fallback enum nếu rỗng)
        model.addAttribute("statusOptions", buildStatusOptions());

        return "orders/list";
    }

    /**
     * Tạo danh sách status (OrderStatusConfig) cho dropdown: lấy từ bảng order_statuses,
     * nếu rỗng thì fallback theo enum OrderStatus.
     */
    private java.util.List<OrderStatusConfig> buildStatusOptions() {
        java.util.List<OrderStatusConfig> out = new java.util.ArrayList<>();
        java.util.List<OrderStatusConfig> configs = orderStatusConfigService.getActive();
        if (configs.isEmpty()) {
            for (OrderStatus os : OrderStatus.values()) {
                OrderStatusConfig fake = new OrderStatusConfig();
                fake.setCode(os.name());
                fake.setName(os.getDisplayName());
                fake.setColor(os.getBootstrapClass());
                fake.setDisplayOrder(os.ordinal());
                out.add(fake);
            }
        } else {
            out.addAll(configs);
        }
        return out;
    }

    // ============ Form tạo đơn hàng mới ============
    @GetMapping("/orders/new")
    public String showCreateForm(Model model) {
        Order order = new Order();
        // Giá trị mặc định
        order.setDoorSystem(DoorSystem.HE_40);
        order.setDoorType(DoorType.CUA_PHONG);
        order.setPattern(DoorPattern.TRON);
        order.setGlassType(GlassType.KHONG);
        order.setQuantity(1);

        model.addAttribute("order", order);
        model.addAttribute("isEdit", false);
        model.addAttribute("doorSystems", DoorSystem.values());
        model.addAttribute("doorTypes", DoorType.values());
        model.addAttribute("patterns", DoorPattern.values());
        model.addAttribute("glassTypes", GlassType.values());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("statusOptions", buildStatusOptions());

        return "orders/create";
    }

    // ============ Form tạo đơn hàng mới (1 đơn → nhiều MẪU cửa → nhiều BỘ cửa) ============
    @GetMapping("/orders/new-multi")
    public String showCreateMultiForm(Model model, HttpSession session) {
        RecentFeaturesTracker.touch(session, RecentFeaturesTracker.FEATURE_CREATE_ORDER);
        Order order = new Order();
        order.setStatus(OrderStatus.CHO_XAC_NHAN.name());

        // Khởi tạo 1 MẪU cửa mặc định với 1 BỘ cửa
        DoorPatternGroup firstGroup = new DoorPatternGroup();
        firstGroup.setDoorSystem(DoorSystem.HE_40);
        firstGroup.setDoorType(DoorType.CUA_PHONG);
        firstGroup.setPattern(DoorPattern.TRON);
        firstGroup.setGlassType(GlassType.KHONG);
        firstGroup.getPatternsSafe().add(DoorPattern.TRON);

        DoorItem firstDoor = new DoorItem();
        firstDoor.setLocation(DoorLocation.PHONG_NGU);
        firstGroup.getDoorsSafe().add(firstDoor);

        List<DoorPatternGroup> groups = new ArrayList<>();
        groups.add(firstGroup);
        order.setPatternGroups(groups);

        model.addAttribute("order", order);
        model.addAttribute("doorSystems", DoorSystem.values());
        model.addAttribute("doorTypes", DoorType.values());
        model.addAttribute("patterns", DoorPattern.values());
        model.addAttribute("glassTypes", GlassType.values());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("statusOptions", buildStatusOptions());
        model.addAttribute("doorLocations", DoorLocation.values());
        // Cho form Bảng cửa
        model.addAttribute("doorSystemConfigs", doorSystemConfigService.getActive());
        model.addAttribute("processingFees", processingFeeService.getActive());
        // Bảng màu cửa (lấy từ color_palette trong DB)
        model.addAttribute("colorPalettes", colorPaletteService.getActive());

        return "orders/create-multi";
    }

    // ============ Lưu đơn hàng (1 đơn → nhiều DÒNG sản phẩm: cửa + phụ kiện + dịch vụ) ============
    @PostMapping(value = "/orders/save-multi", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> saveOrderMulti(@RequestBody Order order) {
        try {
            // Validate
            if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập tên khách hàng"));
            }
            if (order.getCustomerPhone() == null || order.getCustomerPhone().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập số điện thoại"));
            }
            // Validate lineItems (cấu trúc phẳng)
            if (order.getLineItems() != null) {
                List<OrderLineItem> cleaned = new ArrayList<>();
                for (OrderLineItem li : order.getLineItems()) {
                    if (li == null) continue;
                    if (li.getProductName() == null || li.getProductName().isBlank()) continue;
                    cleaned.add(li);
                }
                order.setLineItems(cleaned);
            }
            // Backward-compat: lọc patternGroups cũ
            if (order.getPatternGroups() != null) {
                List<DoorPatternGroup> cleanedGroups = new ArrayList<>();
                for (DoorPatternGroup g : order.getPatternGroups()) {
                    if (g == null) continue;
                    List<DoorItem> cleanedDoors = new ArrayList<>();
                    if (g.getDoors() != null) {
                        for (DoorItem d : g.getDoors()) {
                            if (d != null && (d.getOpeningWidth() != null
                                    || d.getLocation() != null
                                    || d.getLocationDetail() != null)) {
                                cleanedDoors.add(d);
                            }
                        }
                    }
                    g.setDoors(cleanedDoors);
                    if (!cleanedDoors.isEmpty()) cleanedGroups.add(g);
                }
                order.setPatternGroups(cleanedGroups);
            }
            if ((order.getLineItems() == null || order.getLineItems().isEmpty())
                    && (order.getPatternGroups() == null || order.getPatternGroups().isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Vui lòng nhập ít nhất 1 dòng sản phẩm"));
            }
            Order saved = orderService.createOrder(order);
            return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "orderCode", saved.getOrderCode(),
                "groupCount", saved.getLineItems() != null ? saved.getLineItems().size() : 0,
                "doorCount", saved.getDoorLineItemCount(),
                "totalAmount", saved.calculateLineItemsTotal(),
                "redirect", "/orders/detail/" + saved.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ Upload ảnh sản phẩm ============
    @PostMapping(value = "/api/upload", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File rỗng"));
            }
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.')).toLowerCase();
            }
            if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận ảnh (jpg/png/gif/webp)"));
            }
            // Tạo thư mục uploads nếu chưa có
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("src", "main", "resources", "static", "uploads");
            java.nio.file.Files.createDirectories(uploadDir);
            // Tên file: timestamp + random
            String filename = System.currentTimeMillis() + "_" +
                    java.util.UUID.randomUUID().toString().substring(0, 8) + ext;
            java.nio.file.Path target = uploadDir.resolve(filename);
            file.transferTo(target);
            return ResponseEntity.ok(Map.of(
                "url", "/uploads/" + filename,
                "filename", filename
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ Lưu đơn hàng mới ============
    @PostMapping("/orders/save")
    public String saveOrder(@ModelAttribute Order order, RedirectAttributes redirectAttributes) {
        try {
            orderService.createOrder(order);
            redirectAttributes.addFlashAttribute("success", "Tạo đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/orders";
    }

    // ============ Form chỉnh sửa ============ (đã bỏ — chỉnh sửa inline tại trang chi tiết)
    @GetMapping("/orders/edit/{id}")
    public String showEditForm(@PathVariable String id, RedirectAttributes redirectAttributes) {
        // Chuyển hướng sang trang chi tiết (sửa dòng inline tại đó)
        return "redirect:/orders/detail/" + id;
    }

    // ============ Cập nhật đơn hàng ============
    @PostMapping("/orders/update/{id}")
    public String updateOrder(@PathVariable String id, @ModelAttribute Order order, RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrder(id, order);
            redirectAttributes.addFlashAttribute("success", "Cập nhật đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/orders";
    }

    // ============ Xóa đơn hàng ============
    @GetMapping("/orders/delete/{id}")
    public String deleteOrder(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteOrder(id);
            redirectAttributes.addFlashAttribute("success", "Xóa đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/orders";
    }

    // ============ Cập nhật trạng thái (AJAX) ============
    @PostMapping("/orders/{id}/status")
    @ResponseBody
    public String updateStatus(@PathVariable String id, @RequestParam String status) {
        try {
            orderService.updateOrderStatus(id, status);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    // ============ Trang quản lý trạng thái ============
    @GetMapping("/orders/status")
    public String orderStatusBoard(Model model, RedirectAttributes redirectAttributes) {
        try {
            List<Order> all = orderService.getAllOrders();
            // Gom nhóm theo status code (String) từ bảng config
            java.util.Map<String, List<Order>> grouped = new java.util.LinkedHashMap<>();
            java.util.Map<String, OrderStatusConfig> configMap = new java.util.LinkedHashMap<>();
            // 1) Lấy config từ DB (nếu rỗng thì fallback enum)
            List<OrderStatusConfig> configs = orderStatusConfigService.getActive();
            if (configs.isEmpty()) {
                for (OrderStatus s : OrderStatus.values()) {
                    OrderStatusConfig fake = new OrderStatusConfig();
                    fake.setCode(s.name());
                    fake.setName(s.getDisplayName());
                    fake.setColor(s.getBootstrapClass());
                    fake.setDisplayOrder(s.ordinal());
                    configMap.put(s.name(), fake);
                    grouped.put(s.name(), new java.util.ArrayList<>());
                }
            } else {
                for (OrderStatusConfig c : configs) {
                    configMap.put(c.getCode(), c);
                    grouped.put(c.getCode(), new java.util.ArrayList<>());
                }
            }
            // 2) Gom đơn theo status (String)
            for (Order o : all) {
                String key = o.getStatus();
                if (key == null || key.isBlank()) continue;
                grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(o);
            }
            // 3) Tạo entries cho board (cột theo thứ tự config)
            java.util.List<StatusBoardEntry> entries = new java.util.ArrayList<>();
            for (var e : configMap.entrySet()) {
                entries.add(new StatusBoardEntry(e.getValue(), grouped.getOrDefault(e.getKey(), java.util.Collections.emptyList())));
            }
            // 4) StatusOption cho dropdown: tất cả config active
            java.util.List<OrderStatusConfig> statusOptions = new java.util.ArrayList<>(configMap.values());
            model.addAttribute("entries", entries);
            model.addAttribute("statusOptions", statusOptions);
            model.addAttribute("statuses", OrderStatus.values());
            model.addAttribute("totalCount", all.size());
            return "orders/status-board";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/orders";
        }
    }

    // Entry cho board
    public static class StatusBoardEntry {
        private final OrderStatusConfig status;
        private final List<Order> orders;
        public StatusBoardEntry(OrderStatusConfig status, List<Order> orders) {
            this.status = status;
            this.orders = orders;
        }
        public OrderStatusConfig getStatus() { return status; }
        public List<Order> getOrders() { return orders; }
    }

    // ============ Chi tiết đơn hàng ============
    @GetMapping("/orders/detail/{id}")
    public String orderDetail(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderService.getOrderById(id);

        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }
        Order order = orderOpt.get();

        model.addAttribute("order", order);
        model.addAttribute("doorSystems", DoorSystem.values());
        model.addAttribute("doorTypes", DoorType.values());
        model.addAttribute("statuses", OrderStatus.values());
        // Bảng màu cửa cho dropdown trong form edit
        model.addAttribute("colorPalettes", colorPaletteService.getActive());

        // Map phát sinh gia công theo productCode để hiển thị gộp lên bảng báo giá
        // Map<productCode, Map<feeId, ProcessingFee>> để view render cột động theo phát sinh
        java.util.List<ProcessingFee> activeFees = processingFeeService.getActive();
        Map<String, Order.DoorFeeEntry> feeByProductCode = new HashMap<>();
        Map<String, Map<String, ProcessingFee>> lineItemFees = new HashMap<>();
        // Map phẳng: productCode -> feeId -> số tiền đã nhân SL (để Thymeleaf dễ truy xuất)
        Map<String, Map<String, java.math.BigDecimal>> lineItemFeeAmounts = new HashMap<>();
        if (order.getLineItems() != null) {
            Map<String, ProcessingFee> feeById = new HashMap<>();
            for (ProcessingFee pf : activeFees) feeById.put(pf.getId(), pf);
            for (OrderLineItem li : order.getLineItems()) {
                if (li.getProcessingFeeIds() == null) continue;
                Map<String, ProcessingFee> picked = new HashMap<>();
                Map<String, java.math.BigDecimal> amounts = new HashMap<>();
                int qty = li.getQuantity() != null && li.getQuantity() > 0 ? li.getQuantity() : 1;
                for (String fid : li.getProcessingFeeIds()) {
                    ProcessingFee pf = feeById.get(fid);
                    if (pf != null) {
                        picked.put(fid, pf);
                        if (pf.getDefaultUnitPrice() != null) {
                            amounts.put(fid, pf.getDefaultUnitPrice().multiply(java.math.BigDecimal.valueOf(qty)));
                        }
                    }
                }
                if (!picked.isEmpty() && li.getProductCode() != null && !li.getProductCode().isBlank()) {
                    lineItemFees.put(li.getProductCode(), picked);
                    lineItemFeeAmounts.put(li.getProductCode(), amounts);
                }
            }
        }
        model.addAttribute("activeFees", activeFees);
        model.addAttribute("lineItemFees", lineItemFees);
        model.addAttribute("lineItemFeeAmounts", lineItemFeeAmounts);

        // Lọc: chỉ hiển thị cột gia công có phát sinh thực tế trong đơn này
        // (tránh hiển thị 4 cột CNC/Ghép cánh/Chỉ số/Phụ nổi khi đơn không dùng)
        java.util.Set<String> usedFeeIds = new java.util.LinkedHashSet<>();
        for (Map<String, java.math.BigDecimal> amtMap : lineItemFeeAmounts.values()) {
            usedFeeIds.addAll(amtMap.keySet());
        }
        java.util.List<ProcessingFee> usedFees = new java.util.ArrayList<>();
        for (ProcessingFee pf : activeFees) {
            if (usedFeeIds.contains(pf.getId())) usedFees.add(pf);
        }
        model.addAttribute("usedFees", usedFees);

        return "orders/detail";
    }

    // ============ In lệnh SX riêng từng MẪU trong đơn ============
    @GetMapping("/orders/{id}/groups/{groupCode}/print")
    public String printPatternGroup(@PathVariable String id,
                                    @PathVariable String groupCode,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }
        Order order = orderOpt.get();
        DoorPatternGroup target = null;
        if (order.getPatternGroups() != null) {
            for (DoorPatternGroup g : order.getPatternGroups()) {
                if (groupCode.equals(g.getGroupCode())) { target = g; break; }
            }
        }
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy mẫu " + groupCode);
            return "redirect:/orders/detail/" + id;
        }
        model.addAttribute("order", order);
        model.addAttribute("group", target);
        return "orders/print-pattern";
    }

    // ============ Trang quản lý nhiều bộ cửa trong đơn ============
    @GetMapping("/orders/{id}/doors")
    public String manageDoors(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderService.getOrderById(id);

        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        model.addAttribute("order", orderOpt.get());
        model.addAttribute("doorLocations", DoorLocation.values());
        model.addAttribute("doorSystems", DoorSystem.values());
        model.addAttribute("doorTypes", DoorType.values());
        model.addAttribute("patterns", DoorPattern.values());
        model.addAttribute("glassTypes", GlassType.values());

        return "orders/doors";
    }

    // ============ DEV ONLY: lui ngay dat hang de test canh bao ============
    @GetMapping("/dev/shift-order-date/{id}")
    public String shiftOrderDate(@PathVariable String id, @RequestParam int days, RedirectAttributes ra) {
        orderService.shiftOrderDate(id, days);
        ra.addFlashAttribute("success", "Đã lùi " + days + " ngày cho đơn " + id);
        return "redirect:/orders";
    }

    // ============ API tính toán realtime (AJAX) ============
    @PostMapping("/api/calculate")
    @ResponseBody
    public ResponseEntity<ProductionSpec> calculate(
            @RequestParam Integer openingWidth,
            @RequestParam Integer openingHeight,
            @RequestParam Integer wallThickness,
            @RequestParam(required = false, defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String doorSystem,
            @RequestParam(required = false) String doorType) {

        DoorSystem sys = parseEnum(doorSystem, DoorSystem.class, DoorSystem.HE_40);
        DoorType type = parseEnum(doorType, DoorType.class, DoorType.CUA_PHONG);

        ProductionSpec spec = calculator.calculate(openingWidth, openingHeight,
                wallThickness, quantity, sys, type);
        return ResponseEntity.ok(spec);
    }

    // ============ API quản lý nhiều bộ cửa trong đơn ============

    /**
     * Lấy danh sách bộ cửa của 1 đơn (trả về JSON).
     */
    @GetMapping("/api/orders/{id}/doors")
    @ResponseBody
    public ResponseEntity<List<DoorItem>> getDoors(@PathVariable String id) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
        List<DoorItem> doors = orderOpt.get().getAllDoorItems();
        return ResponseEntity.ok(doors);
    }

    /**
     * Thêm 1 bộ cửa mới vào đơn (JSON body).
     */
    @PostMapping("/api/orders/{id}/doors")
    @ResponseBody
    public ResponseEntity<?> addDoor(@PathVariable String id, @RequestBody DoorItem door) {
        try {
            Order updated = orderService.addDoor(id, door);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Cập nhật 1 bộ cửa theo index (JSON body).
     */
    @PutMapping("/api/orders/{id}/doors/{index}")
    @ResponseBody
    public ResponseEntity<?> updateDoor(@PathVariable String id,
                                         @PathVariable int index,
                                         @RequestBody DoorItem door) {
        try {
            Order updated = orderService.updateDoor(id, index, door);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa 1 bộ cửa theo index.
     */
    @DeleteMapping("/api/orders/{id}/doors/{index}")
    @ResponseBody
    public ResponseEntity<?> deleteDoor(@PathVariable String id, @PathVariable int index) {
        try {
            Order updated = orderService.deleteDoor(id, index);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy enum DoorLocation cho frontend (dropdown).
     */
    @GetMapping("/api/door-locations")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getDoorLocations() {
        Map<String, String> map = new HashMap<>();
        for (DoorLocation loc : DoorLocation.values()) {
            map.put(loc.name(), loc.getDisplayName());
        }
        return ResponseEntity.ok(map);
    }

    // ============ CRUD line-items cho chỉnh sửa inline trên trang chi tiết =========
    /** Thêm 1 dòng sản phẩm mới vào bảng báo giá. */
    @PostMapping("/api/orders/{id}/line-items")
    @ResponseBody
    public ResponseEntity<?> addLineItem(@PathVariable String id, @RequestBody OrderLineItem item) {
        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
            Order order = orderOpt.get();
            if (order.getLineItems() == null) order.setLineItems(new ArrayList<>());
            // Tự sinh productCode nếu trống
            if (item.getProductCode() == null || item.getProductCode().isBlank()) {
                String prefix = switch (item.getCategory() != null ? item.getCategory() : ProductCategory.DOOR) {
                    case DOOR -> "DT";
                    case ACCESSORY -> "PK";
                    case SERVICE -> "DV";
                };
                int n = (int) order.getLineItems().stream()
                        .filter(li -> li.getProductCode() != null && li.getProductCode().startsWith(prefix))
                        .count() + 1;
                item.setProductCode(prefix + String.format("%02d", n));
            }
            if (item.getQuantity() == null) item.setQuantity(1);
            if (item.getUnit() == null || item.getUnit().isBlank()) item.setUnit("bộ");
            applyProcessingFeeEntries(item);
            order.getLineItems().add(item);
            // Đồng bộ totalAmount từ line items (đã gồm phụ phí) mỗi khi có line items
            if (order.getLineItems().size() >= 1) {
                order.setTotalAmount(order.calculateLineItemsTotal());
            }
            return ResponseEntity.ok(orderService.saveOrderRaw(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /** Cập nhật 1 dòng theo index trong lineItems. */
    @PutMapping("/api/orders/{id}/line-items/{index}")
    @ResponseBody
    public ResponseEntity<?> updateLineItem(@PathVariable String id,
                                            @PathVariable int index,
                                            @RequestBody OrderLineItem item) {
        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
            Order order = orderOpt.get();
            List<OrderLineItem> list = order.getLineItems();
            if (list == null || index < 0 || index >= list.size()) {
                return ResponseEntity.badRequest().body("Index không hợp lệ");
            }
            if ((item.getProductCode() == null || item.getProductCode().isBlank())
                    && list.get(index).getProductCode() != null) {
                item.setProductCode(list.get(index).getProductCode());
            }
            applyProcessingFeeEntries(item);
            list.set(index, item);
            // Đồng bộ totalAmount từ line items (đã gồm phụ phí)
            order.setTotalAmount(order.calculateLineItemsTotal());
            return ResponseEntity.ok(orderService.saveOrderRaw(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /** Xóa 1 dòng theo index. */
    @DeleteMapping("/api/orders/{id}/line-items/{index}")
    @ResponseBody
    public ResponseEntity<?> deleteLineItem(@PathVariable String id, @PathVariable int index) {
        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
            Order order = orderOpt.get();
            List<OrderLineItem> list = order.getLineItems();
            if (list == null || index < 0 || index >= list.size()) {
                return ResponseEntity.badRequest().body("Index không hợp lệ");
            }
            list.remove(index);
            // Đồng bộ totalAmount từ line items (đã gồm phụ phí)
            order.setTotalAmount(order.calculateLineItemsTotal());
            return ResponseEntity.ok(orderService.saveOrderRaw(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> clazz, E def) {
        if (value == null || value.isBlank()) return def;
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    /**
     * Đồng bộ processingFeeIds / Names / Amount từ processingFeeEntries (nếu client gửi).
     * Nếu không có entries thì giữ logic cũ (server tự tính Amount từ defaultUnitPrice × qty).
     */
    private void applyProcessingFeeEntries(OrderLineItem item) {
        if (item.getProcessingFeeEntries() == null || item.getProcessingFeeEntries().isEmpty()) {
            return; // để code cũ xử lý
        }
        java.util.List<OrderLineItem.ProcessingFeeEntry> entries = item.getProcessingFeeEntries();
        java.util.List<String> ids = new java.util.ArrayList<>();
        java.util.List<String> names = new java.util.ArrayList<>();
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        for (OrderLineItem.ProcessingFeeEntry e : entries) {
            if (e.getFeeId() == null) continue;
            ids.add(e.getFeeId());
            if (e.getFeeName() != null) names.add(e.getFeeName());
            if (e.getAmount() != null) sum = sum.add(e.getAmount());
        }
        item.setProcessingFeeIds(ids);
        item.setProcessingFeeNames(names);
        item.setProcessingFeeAmount(sum);
    }
}