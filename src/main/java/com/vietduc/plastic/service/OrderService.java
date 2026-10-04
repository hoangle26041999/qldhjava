package com.vietduc.plastic.service;

import com.vietduc.plastic.model.*;
import com.vietduc.plastic.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductionCalculatorService calculator;
    private final CustomerService customerService;
    private final ProcessingFeeService processingFeeService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Tạo mã đơn hàng tự động: VD-20261003-001
     */
    public String generateOrderCode() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String prefix = "VD-" + dateStr + "-";

        List<Order> todayOrders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderCode() != null && o.getOrderCode().startsWith(prefix))
                .toList();

        int nextNumber = todayOrders.size() + 1;
        return prefix + String.format("%03d", nextNumber);
    }

    /**
     * Tạo đơn hàng mới — tự động sinh mã, gán mặc định, tính kỹ thuật, lưu KH.
     */
    public Order createOrder(Order order) {
        if (order.getOrderCode() == null || order.getOrderCode().isEmpty()) {
            order.setOrderCode(generateOrderCode());
        }

        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus(OrderStatus.CHO_XAC_NHAN.name());
        }

        // Tự lưu/cập nhật thông tin khách hàng
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
            customerService.findOrCreate(
                    order.getCustomerName(),
                    order.getCustomerPhone(),
                    order.getCustomerAddress(),
                    order.getNotes()
            );
        }

        // Snapshot tên phát sinh gia công + tính phụ phí cộng dồn
        enrichProcessingFees(order);

        // Tính kỹ thuật cho tất cả các mẫu + bộ
        applyCalculations(order);

        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        Order saved = orderRepository.save(order);

        // Tăng đếm đơn cho khách
        if (order.getCustomerPhone() != null) {
            customerService.incrementOrderCount(order.getCustomerPhone());
        }

        return saved;
    }

    /**
     * Resolve processingFeeIds → snapshot names + tính tổng phụ phí (đã nhân SL).
     */
    private void enrichProcessingFees(Order order) {
        if (order.getLineItems() == null) return;
        // Lấy map id → fee 1 lần
        Map<String, ProcessingFee> feeMap = processingFeeService.getAll().stream()
                .collect(Collectors.toMap(ProcessingFee::getId, f -> f));
        for (OrderLineItem li : order.getLineItems()) {
            if (li.getProcessingFeeIds() == null || li.getProcessingFeeIds().isEmpty()) {
                li.setProcessingFeeNames(new ArrayList<>());
                li.setProcessingFeeAmount(BigDecimal.ZERO);
                continue;
            }
            List<String> names = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            int qty = li.getQuantity() != null && li.getQuantity() > 0 ? li.getQuantity() : 1;
            for (String fid : li.getProcessingFeeIds()) {
                ProcessingFee fee = feeMap.get(fid);
                if (fee != null) {
                    names.add(fee.getName());
                    if (fee.getDefaultUnitPrice() != null) {
                        total = total.add(fee.getDefaultUnitPrice().multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }
            li.setProcessingFeeNames(names);
            li.setProcessingFeeAmount(total);
        }
    }

    /**
     * Tính toán kỹ thuật cho cả đơn theo cấu trúc phẳng (lineItems):
     *  Order → [OrderLineItem]  (DOOR + ACCESSORY + SERVICE)
     * <p>
     * - Nếu có lineItems: tính ProductionSpec cho mỗi dòng DOOR có size.
     * - Nếu có patternGroups (legacy): giữ logic cũ.
     * - Nếu có legacy doors (đơn cũ): giữ logic cũ.
     */
    private void applyCalculations(Order order) {
        // Case 0: cấu trúc phẳng mới (lineItems)
        if (order.getLineItems() != null && !order.getLineItems().isEmpty()) {
            int idx = 1;
            for (OrderLineItem item : order.getLineItems()) {
                if (item.getProductCode() == null || item.getProductCode().isEmpty()) {
                    String prefix = item.isDoor() ? "DT" :
                                    item.isAccessory() ? "PK" : "DV";
                    item.setProductCode(order.getOrderCode() + "-" + prefix +
                            String.format("%02d", idx));
                }
                // Tính kỹ thuật cho dòng DOOR có kích thước
                if (item.isDoor() && item.getOpeningWidth() != null
                        && item.getOpeningHeight() != null
                        && item.getWallThickness() != null) {
                    ProductionSpec spec = calculator.calculate(
                            item.getOpeningWidth(),
                            item.getOpeningHeight(),
                            item.getWallThickness(),
                            item.getQuantity() != null ? item.getQuantity() : 1,
                            item.getDoorSystem() != null ? item.getDoorSystem() : DoorSystem.HE_40,
                            item.getDoorType() != null ? item.getDoorType() : DoorType.CUA_PHONG
                    );
                    applySpecToLineItem(item, spec);
                }
                // Tính thành tiền tự động
                if (item.getUnitPrice() != null) {
                    item.setTotalPrice(item.calculateTotalPrice());
                }
                idx++;
            }
            // Đồng bộ field flat (lấy DOOR đầu tiên) cho view cũ
            syncFlatFieldsFromFirstDoor(order);
            return;
        }

        // Case 1: cấu trúc 2 cấp cũ (patternGroups) — backward-compat
        if (order.getPatternGroups() != null && !order.getPatternGroups().isEmpty()) {
            int groupIdx = 1;
            for (DoorPatternGroup group : order.getPatternGroups()) {
                if (group.getGroupCode() == null || group.getGroupCode().isEmpty()) {
                    group.setGroupCode(DoorPatternGroup.generateGroupCode(order.getOrderCode(), groupIdx));
                }
                // Auto-suy extra* từ patterns của mẫu
                group.inferExtraFeesFromPatterns();

                // Tính riêng cho từng bộ cửa
                int itemIdx = 1;
                if (group.getDoors() != null) {
                    for (DoorItem door : group.getDoors()) {
                        if (door.getItemCode() == null || door.getItemCode().isEmpty()) {
                            door.setItemCode(DoorItem.generateItemCode(group.getGroupCode(), itemIdx));
                        }
                        // Mỗi DoorItem = 1 bộ cửa (quantity=1)
                        ProductionSpec spec = calculator.calculate(
                                door.getOpeningWidth(),
                                door.getOpeningHeight(),
                                door.getWallThickness(),
                                1,
                                group.getDoorSystem() != null ? group.getDoorSystem() : DoorSystem.HE_40,
                                group.getDoorType() != null ? group.getDoorType() : DoorType.CUA_PHONG
                        );
                        applySpecToDoorItem(door, spec);
                        itemIdx++;
                    }
                }
                groupIdx++;
            }
            // Đồng bộ field flat (lấy bộ đầu tiên của mẫu đầu tiên) cho view cũ
            syncFlatFieldsFromFirstGroup(order);
            return;
        }

        // Case 2: legacy doors (đơn cũ, 1 danh sách DoorItem flat)
        if (order.getDoors() != null && !order.getDoors().isEmpty()) {
            int idx = 1;
            for (DoorItem door : order.getDoors()) {
                if (door.getItemCode() == null || door.getItemCode().isEmpty()) {
                    door.setItemCode(DoorItem.generateItemCode(order.getOrderCode(), idx));
                }
                ProductionSpec spec = calculator.calculate(
                        door.getOpeningWidth(),
                        door.getOpeningHeight(),
                        door.getWallThickness(),
                        1,
                        order.getDoorSystem() != null ? order.getDoorSystem() : DoorSystem.HE_40,
                        order.getDoorType() != null ? order.getDoorType() : DoorType.CUA_PHONG
                );
                applySpecToDoorItem(door, spec);
                idx++;
            }
            syncFlatFieldsFromFirstDoor(order);
            return;
        }

        // Case 3: logic cũ - 1 đơn = 1 bộ
        ProductionSpec spec = calculator.calculate(
                order.getOpeningWidth(),
                order.getOpeningHeight(),
                order.getWallThickness(),
                order.getQuantity(),
                order.getDoorSystem(),
                order.getDoorType()
        );
        applySpecToOrder(order, spec);
    }

    /**
     * Copy ProductionSpec vào các field nhanh của OrderLineItem.
     */
    private void applySpecToLineItem(OrderLineItem item, ProductionSpec spec) {
        item.setProductionSpec(spec);
        item.setLeafWidth(spec.getLeafWidth());
        item.setLeafHeight(spec.getLeafHeight());
        item.setVerticalFrame(spec.getVerticalFrame());
        item.setHorizontalFrame(spec.getHorizontalFrame());
        item.setHeightFrame(spec.getHeightFrame());
        item.setVerticalTrim(spec.getVerticalTrim());
        item.setVerticalTrimQuantity(spec.getVerticalTrimQuantity());
        item.setHorizontalTrim(spec.getHorizontalTrim());
        item.setNeedsReview(spec.getNeedsReview());
    }

    /**
     * Copy ProductionSpec vào các field nhanh của DoorItem.
     */
    private void applySpecToDoorItem(DoorItem door, ProductionSpec spec) {
        door.setProductionSpec(spec);
        door.setLeafWidth(spec.getLeafWidth());
        door.setLeafHeight(spec.getLeafHeight());
        door.setVerticalFrame(spec.getVerticalFrame());
        door.setHorizontalFrame(spec.getHorizontalFrame());
        door.setHeightFrame(spec.getHeightFrame());
        door.setVerticalTrim(spec.getVerticalTrim());
        door.setVerticalTrimQuantity(spec.getVerticalTrimQuantity());
        door.setHorizontalTrim(spec.getHorizontalTrim());
        door.setNeedsReview(spec.getNeedsReview());
    }

    /**
     * Đồng bộ field flat từ bộ cửa đầu tiên của mẫu đầu tiên
     * (cho các view/list chưa refactor).
     */
    private void syncFlatFieldsFromFirstGroup(Order order) {
        if (order.getPatternGroups() == null || order.getPatternGroups().isEmpty()) return;
        DoorPatternGroup firstGroup = order.getPatternGroups().get(0);
        order.setDoorSystem(firstGroup.getDoorSystem());
        order.setDoorType(firstGroup.getDoorType());
        order.setPattern(firstGroup.getPattern());
        order.setColorName(firstGroup.getColorName());
        order.setGlassType(firstGroup.getGlassType());

        if (firstGroup.getDoors() != null && !firstGroup.getDoors().isEmpty()) {
            DoorItem first = firstGroup.getDoors().get(0);
            order.setOpeningWidth(first.getOpeningWidth());
            order.setOpeningHeight(first.getOpeningHeight());
            order.setWallThickness(first.getWallThickness());
            order.setQuantity(order.getTotalDoorCount());
            applySpecToOrder(order, first.getProductionSpec() != null
                    ? first.getProductionSpec()
                    : new ProductionSpec());
        }
    }

    /**
     * Đồng bộ field flat từ bộ cửa đầu tiên (legacy).
     */
    private void syncFlatFieldsFromFirstDoor(Order order) {
        if (order.getDoors() == null || order.getDoors().isEmpty()) return;
        DoorItem first = order.getDoors().get(0);
        order.setQuantity(order.getTotalDoorCount());
        applySpecToOrder(order, first.getProductionSpec() != null
                ? first.getProductionSpec()
                : new ProductionSpec());
    }

    /**
     * Cập nhật đơn hàng — tính lại thông số kỹ thuật.
     */
    public Order updateOrder(String id, Order updatedOrder) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        // Thông tin khách hàng
        existing.setCustomerName(updatedOrder.getCustomerName());
        existing.setCustomerPhone(updatedOrder.getCustomerPhone());
        existing.setCustomerAddress(updatedOrder.getCustomerAddress());
        existing.setNotes(updatedOrder.getNotes());
        existing.setTotalAmount(updatedOrder.getTotalAmount());
        existing.setStatus(updatedOrder.getStatus());

        // Cập nhật cấu trúc 2 cấp (mới)
        if (updatedOrder.getPatternGroups() != null) {
            existing.setPatternGroups(updatedOrder.getPatternGroups());
        }
        // Cập nhật legacy doors (cũ)
        if (updatedOrder.getDoors() != null) {
            existing.setDoors(updatedOrder.getDoors());
        }
        // Backward-compat: cập nhật field flat
        existing.setDoorSystem(updatedOrder.getDoorSystem());
        existing.setDoorType(updatedOrder.getDoorType());
        existing.setPattern(updatedOrder.getPattern());
        existing.setColorName(updatedOrder.getColorName());
        existing.setGlassType(updatedOrder.getGlassType());
        existing.setOpeningWidth(updatedOrder.getOpeningWidth());
        existing.setOpeningHeight(updatedOrder.getOpeningHeight());
        existing.setWallThickness(updatedOrder.getWallThickness());
        existing.setQuantity(updatedOrder.getQuantity());
        existing.setExtraScrew(updatedOrder.getExtraScrew());
        existing.setExtraCnc(updatedOrder.getExtraCnc());
        existing.setExtraGlassShort(updatedOrder.getExtraGlassShort());
        existing.setExtraGlassLong(updatedOrder.getExtraGlassLong());
        existing.setExtraVent(updatedOrder.getExtraVent());
        existing.setExtraAluminum(updatedOrder.getExtraAluminum());
        existing.setExtraMolding(updatedOrder.getExtraMolding());
        existing.setExtraPaintDoor(updatedOrder.getExtraPaintDoor());
        existing.setExtraBo(updatedOrder.getExtraBo());
        existing.setExtraMoldingGia(updatedOrder.getExtraMoldingGia());
        existing.setExtraLock(updatedOrder.getExtraLock());
        existing.setExtraColor(updatedOrder.getExtraColor());
        existing.setLockType(updatedOrder.getLockType());
        existing.setLockQuantity(updatedOrder.getLockQuantity());
        existing.setHingeType(updatedOrder.getHingeType());
        existing.setHingeQuantity(updatedOrder.getHingeQuantity());
        existing.setGlassQuantity(updatedOrder.getGlassQuantity());

        applyCalculations(existing);
        existing.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(existing);
    }

    public Order updateOrderStatus(String id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public void deleteOrder(String id) {
        orderRepository.deleteById(id);
    }

    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        orders.sort((a, b) -> {
            LocalDateTime ad = a.getOrderDate() != null ? a.getOrderDate() : a.getCreatedAt();
            LocalDateTime bd = b.getOrderDate() != null ? b.getOrderDate() : b.getCreatedAt();
            if (ad == null && bd == null) return 0;
            if (ad == null) return 1;
            if (bd == null) return -1;
            return bd.compareTo(ad);
        });
        return orders;
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    /**
     * Lưu trực tiếp đơn (dùng cho API thêm/sửa/xóa lineItems inline, không qua logic business phức tạp).
     */
    public Order saveOrderRaw(Order order) {
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Optional<Order> getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }

    public List<Order> searchOrders(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllOrders();
        }
        List<Order> byName = orderRepository.findByCustomerNameContainingIgnoreCase(keyword);
        List<Order> byPhone = orderRepository.findByCustomerPhoneContaining(keyword);
        byName.addAll(byPhone);
        return byName.stream().distinct().toList();
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByOrderDateBetween(startDate, endDate);
    }

    public long countOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).size();
    }

    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findByStatus("HOAN_THANH").stream()
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * DEV ONLY: lui/tăng orderDate để test cảnh báo deadline.
     */
    public void shiftOrderDate(String id, int days) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang: " + id));
        LocalDateTime current = order.getOrderDate() != null
                ? order.getOrderDate()
                : LocalDateTime.now();
        order.setOrderDate(current.plusDays(days));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    /**
     * Copy ProductionSpec vào các field nhanh của Order.
     */
    private void applySpecToOrder(Order order, ProductionSpec spec) {
        order.setProductionSpec(spec);
        order.setLeafWidth(spec.getLeafWidth());
        order.setLeafHeight(spec.getLeafHeight());
        order.setVerticalFrame(spec.getVerticalFrame());
        order.setHorizontalFrame(spec.getHorizontalFrame());
        order.setHeightFrame(spec.getHeightFrame());
        order.setVerticalTrim(spec.getVerticalTrim());
        order.setVerticalTrimQuantity(spec.getVerticalTrimQuantity());
        order.setHorizontalTrim(spec.getHorizontalTrim());
        order.setNeedsReview(spec.getNeedsReview());
    }

    // ============ API: quản lý DoorPatternGroup + DoorItem ============

    /**
     * Thêm 1 MẪU cửa mới vào đơn.
     */
    public Order addPatternGroup(String orderId, DoorPatternGroup group) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getPatternGroups() == null) {
            order.setPatternGroups(new ArrayList<>());
        }

        // Auto-sinh groupCode
        if (group.getGroupCode() == null || group.getGroupCode().isEmpty()) {
            group.setGroupCode(DoorPatternGroup.generateGroupCode(
                    order.getOrderCode(), order.getPatternGroups().size() + 1));
        }
        group.inferExtraFeesFromPatterns();

        // Tính spec cho từng bộ trong mẫu
        if (group.getDoors() != null) {
            int idx = 1;
            for (DoorItem door : group.getDoors()) {
                if (door.getItemCode() == null || door.getItemCode().isEmpty()) {
                    door.setItemCode(DoorItem.generateItemCode(group.getGroupCode(), idx));
                }
                ProductionSpec spec = calculator.calculate(
                        door.getOpeningWidth(),
                        door.getOpeningHeight(),
                        door.getWallThickness(),
                        1,
                        group.getDoorSystem() != null ? group.getDoorSystem() : DoorSystem.HE_40,
                        group.getDoorType() != null ? group.getDoorType() : DoorType.CUA_PHONG
                );
                applySpecToDoorItem(door, spec);
                idx++;
            }
        }

        order.getPatternGroups().add(group);
        order.setUpdatedAt(LocalDateTime.now());
        syncFlatFieldsFromFirstGroup(order);
        return orderRepository.save(order);
    }

    /**
     * Thêm 1 BỘ cửa vào MẪU cửa (theo index mẫu).
     */
    public Order addDoorToGroup(String orderId, int groupIndex, DoorItem door) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getPatternGroups() == null || groupIndex < 0
                || groupIndex >= order.getPatternGroups().size()) {
            throw new RuntimeException("Mẫu cửa không tồn tại (index=" + groupIndex + ")");
        }
        DoorPatternGroup group = order.getPatternGroups().get(groupIndex);

        if (group.getDoors() == null) group.setDoors(new ArrayList<>());

        // Auto itemCode
        if (door.getItemCode() == null || door.getItemCode().isEmpty()) {
            door.setItemCode(DoorItem.generateItemCode(
                    group.getGroupCode() != null ? group.getGroupCode() : order.getOrderCode(),
                    group.getDoors().size() + 1));
        }

        ProductionSpec spec = calculator.calculate(
                door.getOpeningWidth(),
                door.getOpeningHeight(),
                door.getWallThickness(),
                1,
                group.getDoorSystem() != null ? group.getDoorSystem() : DoorSystem.HE_40,
                group.getDoorType() != null ? group.getDoorType() : DoorType.CUA_PHONG
        );
        applySpecToDoorItem(door, spec);

        group.getDoors().add(door);
        order.setUpdatedAt(LocalDateTime.now());
        syncFlatFieldsFromFirstGroup(order);
        return orderRepository.save(order);
    }

    /**
     * Cập nhật 1 BỘ cửa trong 1 MẪU.
     */
    public Order updateDoorInGroup(String orderId, int groupIndex, int doorIndex, DoorItem door) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getPatternGroups() == null || groupIndex < 0
                || groupIndex >= order.getPatternGroups().size()) {
            throw new RuntimeException("Mẫu cửa không tồn tại");
        }
        DoorPatternGroup group = order.getPatternGroups().get(groupIndex);
        if (group.getDoors() == null || doorIndex < 0 || doorIndex >= group.getDoors().size()) {
            throw new RuntimeException("Bộ cửa không tồn tại");
        }
        DoorItem existing = group.getDoors().get(doorIndex);

        // Copy các field từ door vào existing (giữ itemCode)
        existing.setLocation(door.getLocation());
        existing.setLocationDetail(door.getLocationDetail());
        existing.setOpeningWidth(door.getOpeningWidth());
        existing.setOpeningHeight(door.getOpeningHeight());
        existing.setWallThickness(door.getWallThickness());
        existing.setLockType(door.getLockType());
        existing.setLockQuantity(door.getLockQuantity());
        existing.setHingeType(door.getHingeType());
        existing.setHingeQuantity(door.getHingeQuantity());
        existing.setGlassQuantity(door.getGlassQuantity());
        existing.setUnitPrice(door.getUnitPrice());

        ProductionSpec spec = calculator.calculate(
                existing.getOpeningWidth(),
                existing.getOpeningHeight(),
                existing.getWallThickness(),
                1,
                group.getDoorSystem() != null ? group.getDoorSystem() : DoorSystem.HE_40,
                group.getDoorType() != null ? group.getDoorType() : DoorType.CUA_PHONG
        );
        applySpecToDoorItem(existing, spec);

        order.setUpdatedAt(LocalDateTime.now());
        syncFlatFieldsFromFirstGroup(order);
        return orderRepository.save(order);
    }

    /**
     * Xóa 1 BỘ cửa trong 1 MẪU.
     */
    public Order deleteDoorInGroup(String orderId, int groupIndex, int doorIndex) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getPatternGroups() == null || groupIndex < 0
                || groupIndex >= order.getPatternGroups().size()) {
            throw new RuntimeException("Mẫu cửa không tồn tại");
        }
        DoorPatternGroup group = order.getPatternGroups().get(groupIndex);
        if (group.getDoors() == null || doorIndex < 0 || doorIndex >= group.getDoors().size()) {
            throw new RuntimeException("Bộ cửa không tồn tại");
        }
        group.getDoors().remove(doorIndex);

        // Re-index itemCode
        if (group.getDoors() != null && group.getGroupCode() != null) {
            int idx = 1;
            for (DoorItem d : group.getDoors()) {
                d.setItemCode(DoorItem.generateItemCode(group.getGroupCode(), idx));
                idx++;
            }
        }

        order.setUpdatedAt(LocalDateTime.now());
        syncFlatFieldsFromFirstGroup(order);
        return orderRepository.save(order);
    }

    /**
     * Xóa 1 MẪU cửa.
     */
    public Order deletePatternGroup(String orderId, int groupIndex) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getPatternGroups() == null || groupIndex < 0
                || groupIndex >= order.getPatternGroups().size()) {
            throw new RuntimeException("Mẫu cửa không tồn tại");
        }
        order.getPatternGroups().remove(groupIndex);

        // Re-index groupCode
        if (order.getPatternGroups() != null) {
            int idx = 1;
            for (DoorPatternGroup g : order.getPatternGroups()) {
                g.setGroupCode(DoorPatternGroup.generateGroupCode(order.getOrderCode(), idx));
                if (g.getDoors() != null) {
                    int j = 1;
                    for (DoorItem d : g.getDoors()) {
                        d.setItemCode(DoorItem.generateItemCode(g.getGroupCode(), j));
                        j++;
                    }
                }
                idx++;
            }
        }

        order.setUpdatedAt(LocalDateTime.now());
        syncFlatFieldsFromFirstGroup(order);
        return orderRepository.save(order);
    }

    // ============ Legacy API: giữ để không break controller cũ ============

    /**
     * Thêm 1 bộ cửa (legacy - thêm vào mẫu đầu tiên hoặc tạo mẫu mới).
     */
    public Order addDoor(String orderId, DoorItem door) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        // Đảm bảo có ít nhất 1 mẫu
        if (order.getPatternGroups() == null || order.getPatternGroups().isEmpty()) {
            DoorPatternGroup newGroup = new DoorPatternGroup();
            newGroup.setDoorSystem(order.getDoorSystem());
            newGroup.setDoorType(order.getDoorType());
            newGroup.setPattern(order.getPattern());
            newGroup.setColorName(order.getColorName());
            newGroup.setGlassType(order.getGlassType());
            return addPatternGroup(orderId, copyDoorIntoNewGroup(newGroup, door));
        }
        return addDoorToGroup(orderId, 0, door);
    }

    private DoorPatternGroup copyDoorIntoNewGroup(DoorPatternGroup group, DoorItem door) {
        if (group.getDoors() == null) group.setDoors(new ArrayList<>());
        group.getDoors().add(door);
        return group;
    }

    public Order updateDoor(String orderId, int index, DoorItem door) {
        return updateDoorInGroup(orderId, 0, index, door);
    }

    public Order deleteDoor(String orderId, int index) {
        return deleteDoorInGroup(orderId, 0, index);
    }
}