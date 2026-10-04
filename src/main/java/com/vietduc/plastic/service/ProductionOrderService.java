package com.vietduc.plastic.service;

import com.vietduc.plastic.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tạo LỆNH SẢN XUẤT theo nhóm.
 * <p>
 * Luồng:
 *   1. Lấy tất cả các đơn (hoặc đơn đã chọn).
 *   2. Trích xuất tất cả BỘ CỬA từ các đơn:
 *      - Ưu tiên lineItems DOOR (cấu trúc phẳng mới).
 *      - Sau đó tới patternGroups → doors (cấu trúc 2 cấp cũ).
 *      - Cuối cùng legacy doors (flat).
 *   3. Tính / lấy ProductionSpec cho mỗi bộ.
 *   4. GOM NHÓM theo "quy cách SX" — gồm:
 *          hệ cánh + loại cửa + mẫu + màu + ô kính
 *          + cánh R SX + cánh C SX
 *          + khuôn đứng + khuôn ngang + khuôn cao
 *          + nẹp đứng + nẹp ngang
 *          + nẹp chống hở (nếu có)
 *      Hai bộ khác bất kỳ chi tiết nào → tách nhóm.
 *   5. Mỗi nhóm giữ danh sách bộ (kèm mã đơn + mã bộ + vị trí) để truy ngược khi giao.
 *   6. Sắp xếp nhóm: theo hệ → loại cửa → cánh R → cánh C → mẫu → màu.
 */
@Service
@RequiredArgsConstructor
public class ProductionOrderService {

    private final OrderService orderService;
    private final ProductionCalculatorService calculator;

    /**
     * Tạo báo cáo lệnh SX cho tập đơn đã chọn (mặc định = tất cả đơn chưa hoàn thành/hủy).
     */
    public ProductionOrderReport buildReport(List<Order> orders) {
        ProductionOrderReport report = new ProductionOrderReport();

        // Lọc đơn hợp lệ
        List<Order> active = orders.stream()
                .filter(o -> !"DA_HUY".equals(o.getStatus()) && !"HOAN_THANH".equals(o.getStatus()))
                .toList();
        report.setSourceOrderCount(active.size());

        // Trích xuất bộ cửa
        List<DoorEntry> allDoors = new ArrayList<>();
        for (Order o : active) {
            allDoors.addAll(extractDoors(o));
        }
        report.setTotalDoorCount(allDoors.size());

        // Gom nhóm theo quy cách SX
        Map<SpecKey, List<DoorEntry>> grouped = new LinkedHashMap<>();
        for (DoorEntry de : allDoors) {
            SpecKey key = specKeyOf(de);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(de);
        }

        // Chuyển thành danh sách ProductionGroup, sắp xếp
        List<ProductionGroup> groups = new ArrayList<>();
        int stt = 1;
        for (var entry : grouped.entrySet()) {
            ProductionGroup g = new ProductionGroup();
            g.setStt(stt++);
            g.setSpecKey(entry.getKey());
            g.setDoors(entry.getValue());
            // Sắp xếp các bộ trong nhóm theo mã đơn + mã bộ cho dễ truy ngược
            entry.getValue().sort(Comparator
                    .comparing(DoorEntry::getOrderCode, Comparator.nullsLast(String::compareTo))
                    .thenComparing(DoorEntry::getItemCode, Comparator.nullsLast(String::compareTo)));
            groups.add(g);
        }
        // Sắp xếp nhóm theo hệ → loại cửa → cánh → mẫu → màu
        groups.sort(Comparator
                .comparing((ProductionGroup g) -> nullSafe(g.getSpecKey().doorSystem))
                .thenComparing(g -> nullSafe(g.getSpecKey().doorType))
                .thenComparing(g -> nullSafeInt(g.getSpecKey().leafWidth))
                .thenComparing(g -> nullSafeInt(g.getSpecKey().leafHeight))
                .thenComparing(g -> nullSafe(g.getSpecKey().pattern))
                .thenComparing(g -> nullSafe(g.getSpecKey().colorName))
                .thenComparing(g -> nullSafe(g.getSpecKey().glassType))
        );
        report.setGroups(groups);

        // Tổng vật tư (tổng hợp)
        report.setMaterialSummary(buildMaterialSummary(groups));

        return report;
    }

    /**
     * Trích xuất tất cả DoorEntry từ một Order, hỗ trợ cả 3 cấu trúc:
     *  1. lineItems (DOOR) → đơn vị tính là quantity của lineItem (có thể > 1)
     *  2. patternGroups → doors (mỗi door = 1 bộ, quantity=1)
     *  3. legacy doors (đơn cũ flat)
     */
    private List<DoorEntry> extractDoors(Order order) {
        List<DoorEntry> out = new ArrayList<>();
        String orderCode = order.getOrderCode();

        // ==== Case 1: lineItems (DOOR) ====
        if (order.getLineItems() != null && !order.getLineItems().isEmpty()) {
            for (OrderLineItem li : order.getLineItems()) {
                if (!li.isDoor()) continue;
                int qty = li.getQuantity() != null && li.getQuantity() > 0 ? li.getQuantity() : 1;
                ProductionSpec spec = li.getProductionSpec();
                if (spec == null && li.getOpeningWidth() != null && li.getOpeningHeight() != null
                        && li.getWallThickness() != null) {
                    spec = calculator.calculate(
                            li.getOpeningWidth(), li.getOpeningHeight(), li.getWallThickness(),
                            qty,
                            li.getDoorSystem() != null ? li.getDoorSystem() : DoorSystem.HE_40,
                            li.getDoorType() != null ? li.getDoorType() : DoorType.CUA_PHONG);
                    applySpecToLineItem(li, spec);
                }
                if (spec == null) continue; // bỏ bộ thiếu size
                // Lấy pattern từ lineItem
                List<DoorPattern> patterns = new ArrayList<>();
                if (li.getPattern() != null) patterns.add(li.getPattern());
                DoorEntry de = new DoorEntry();
                de.setOrderId(order.getId());
                de.setOrderCode(orderCode);
                de.setItemCode(li.getProductCode());
                de.setLocation(li.getLocation());
                de.setLocationDetail(li.getLocationDetail());
                de.setDoorSystem(li.getDoorSystem());
                de.setDoorType(li.getDoorType());
                de.setPatterns(patterns);
                de.setColorName(li.getColorName());
                de.setGlassType(li.getGlassType());
                de.setQuantity(qty);
                de.setSpec(spec);
                de.setNotes(li.getNotes());
                out.add(de);
            }
            if (!out.isEmpty()) return out;
        }

        // ==== Case 2: patternGroups → doors ====
        if (order.getPatternGroups() != null && !order.getPatternGroups().isEmpty()) {
            for (DoorPatternGroup g : order.getPatternGroups()) {
                if (g.getDoors() == null) continue;
                for (DoorItem door : g.getDoors()) {
                    ProductionSpec spec = door.getProductionSpec();
                    if (spec == null && door.getOpeningWidth() != null && door.getOpeningHeight() != null
                            && door.getWallThickness() != null) {
                        spec = calculator.calculate(
                                door.getOpeningWidth(), door.getOpeningHeight(), door.getWallThickness(),
                                1,
                                g.getDoorSystem() != null ? g.getDoorSystem() : DoorSystem.HE_40,
                                g.getDoorType() != null ? g.getDoorType() : DoorType.CUA_PHONG);
                        applySpecToDoorItem(door, spec);
                    }
                    if (spec == null) continue;
                    DoorEntry de = new DoorEntry();
                    de.setOrderId(order.getId());
                    de.setOrderCode(orderCode);
                    de.setItemCode(door.getItemCode());
                    de.setLocation(door.getLocation());
                    de.setLocationDetail(door.getLocationDetail());
                    de.setDoorSystem(g.getDoorSystem());
                    de.setDoorType(g.getDoorType());
                    List<DoorPattern> patterns = new ArrayList<>();
                    if (g.getPatterns() != null) patterns.addAll(g.getPatterns());
                    else if (g.getPattern() != null) patterns.add(g.getPattern());
                    de.setPatterns(patterns);
                    de.setColorName(g.getColorName());
                    de.setGlassType(g.getGlassType());
                    de.setQuantity(door.getQuantity() != null && door.getQuantity() > 0 ? door.getQuantity() : 1);
                    de.setSpec(spec);
                    out.add(de);
                }
            }
            if (!out.isEmpty()) return out;
        }

        // ==== Case 3: legacy doors ====
        if (order.getDoors() != null && !order.getDoors().isEmpty()) {
            for (DoorItem door : order.getDoors()) {
                ProductionSpec spec = door.getProductionSpec();
                if (spec == null && door.getOpeningWidth() != null && door.getOpeningHeight() != null
                        && door.getWallThickness() != null) {
                    spec = calculator.calculate(
                            door.getOpeningWidth(), door.getOpeningHeight(), door.getWallThickness(),
                            1,
                            order.getDoorSystem() != null ? order.getDoorSystem() : DoorSystem.HE_40,
                            order.getDoorType() != null ? order.getDoorType() : DoorType.CUA_PHONG);
                    applySpecToDoorItem(door, spec);
                }
                if (spec == null) continue;
                DoorEntry de = new DoorEntry();
                de.setOrderId(order.getId());
                de.setOrderCode(orderCode);
                de.setItemCode(door.getItemCode());
                de.setLocation(door.getLocation());
                de.setLocationDetail(door.getLocationDetail());
                de.setDoorSystem(order.getDoorSystem());
                de.setDoorType(order.getDoorType());
                de.setPatterns(order.getPattern() != null ? List.of(order.getPattern()) : List.of());
                de.setColorName(order.getColorName());
                de.setGlassType(order.getGlassType());
                de.setQuantity(door.getQuantity() != null && door.getQuantity() > 0 ? door.getQuantity() : 1);
                de.setSpec(spec);
                out.add(de);
            }
        }

        return out;
    }

    /**
     * Tạo khóa nhóm (SpecKey) từ DoorEntry.
     * Hai bộ có cùng khóa ⇒ cùng nhóm sản xuất.
     */
    private SpecKey specKeyOf(DoorEntry de) {
        SpecKey k = new SpecKey();
        ProductionSpec s = de.getSpec();
        k.doorSystem = de.getDoorSystem();
        k.doorType = de.getDoorType();
        // Ghép pattern thành 1 chuỗi để so sánh (đã sort để tránh thứ tự khác)
        List<String> pNames = de.getPatterns() == null ? List.of()
                : de.getPatterns().stream().map(p -> p == null ? "" : p.name()).sorted().toList();
        k.pattern = String.join("+", pNames);
        k.colorName = de.getColorName() == null ? "" : de.getColorName().trim();
        k.glassType = de.getGlassType();
        k.leafWidth = s.getLeafWidth();
        k.leafHeight = s.getLeafHeight();
        k.verticalFrame = s.getVerticalFrame();
        k.horizontalFrame = s.getHorizontalFrame();
        k.heightFrame = s.getHeightFrame();
        k.verticalTrim = s.getVerticalTrim();
        k.horizontalTrim = s.getHorizontalTrim();
        k.antiGapTrimQuantity = s.getAntiGapTrimQuantity();
        k.wallThickness = s.getWallThickness();
        return k;
    }

    /**
     * Tính bảng tổng hợp vật tư từ các nhóm.
     * Gồm: cánh, khuôn đứng, khuôn ngang, khuôn cao, nẹp đứng, nẹp ngang, nẹp chống hở.
     */
    private MaterialSummary buildMaterialSummary(List<ProductionGroup> groups) {
        MaterialSummary m = new MaterialSummary();

        int totalLeaves = 0;
        int totalBo = 0;
        Map<Integer, Integer> verticalFrameQty = new TreeMap<>();
        Map<Integer, Integer> horizontalFrameQty = new TreeMap<>();
        Map<Integer, Integer> heightFrameQty = new TreeMap<>();
        Map<Integer, Integer> verticalTrimQty = new TreeMap<>();
        Map<Integer, Integer> horizontalTrimQty = new TreeMap<>();
        Map<Integer, Integer> antiGapTrimQty = new TreeMap<>();

        for (ProductionGroup g : groups) {
            ProductionSpec s = g.getSpecKey() == null ? null : null;
            SpecKey k = g.getSpecKey();
            int bo = g.getDoors().size(); // số bộ trong nhóm
            int qtyOfDoors = g.getDoors().stream().mapToInt(d -> d.getQuantity()).sum();
            totalBo += qtyOfDoors;
            totalLeaves += qtyOfDoors; // 1 bộ = 1 cánh (mặc định)

            // Khuôn đứng: 2 thanh / bộ
            if (k.verticalFrame != null) {
                verticalFrameQty.merge(k.verticalFrame, qtyOfDoors * 2, Integer::sum);
            }
            // Khuôn ngang: 1 thanh / bộ (thường là 1 thanh ngang trên + 1 thanh dưới = 2,
            // nhưng khuôn ngang thường tính 1 thanh duy nhất cho cánh. Ở đây lấy 1.)
            if (k.horizontalFrame != null) {
                horizontalFrameQty.merge(k.horizontalFrame, qtyOfDoors, Integer::sum);
            }
            // Khuôn cao: 1 thanh / bộ (khuôn ngang phía trên)
            if (k.heightFrame != null) {
                heightFrameQty.merge(k.heightFrame, qtyOfDoors, Integer::sum);
            }
            // Nẹp đứng: 4 thanh / bộ (theo spec)
            if (k.verticalTrim != null) {
                verticalTrimQty.merge(k.verticalTrim, qtyOfDoors * 4, Integer::sum);
            }
            // Nẹp ngang: 1 thanh / bộ (nếu có)
            if (k.horizontalTrim != null) {
                horizontalTrimQty.merge(k.horizontalTrim, qtyOfDoors, Integer::sum);
            }
            // Nẹp chống hở (cho bộ gió / cửa đôi): lấy từ spec đã tính sẵn trên từng bộ
            int antiGapTotal = 0;
            for (DoorEntry de : g.getDoors()) {
                Integer agq = de.getSpec() != null ? de.getSpec().getAntiGapTrimQuantity() : null;
                if (agq != null) antiGapTotal += agq * de.getQuantity();
            }
            if (antiGapTotal > 0) {
                antiGapTrimQty.merge(0, antiGapTotal, Integer::sum); // gom chung vào key=0 (không theo chiều dài)
            }
        }

        m.setTotalBo(totalBo);
        m.setTotalLeaves(totalLeaves);
        m.setVerticalFrameQty(verticalFrameQty);
        m.setHorizontalFrameQty(horizontalFrameQty);
        m.setHeightFrameQty(heightFrameQty);
        m.setVerticalTrimQty(verticalTrimQty);
        m.setHorizontalTrimQty(horizontalTrimQty);
        m.setAntiGapTrimQty(antiGapTrimQty);
        // Tổng cộng (để hiển thị)
        m.setTotalVerticalFrame(verticalFrameQty.values().stream().mapToInt(Integer::intValue).sum());
        m.setTotalHorizontalFrame(horizontalFrameQty.values().stream().mapToInt(Integer::intValue).sum());
        m.setTotalHeightFrame(heightFrameQty.values().stream().mapToInt(Integer::intValue).sum());
        m.setTotalVerticalTrim(verticalTrimQty.values().stream().mapToInt(Integer::intValue).sum());
        m.setTotalHorizontalTrim(horizontalTrimQty.values().stream().mapToInt(Integer::intValue).sum());
        m.setTotalAntiGapTrim(antiGapTrimQty.values().stream().mapToInt(Integer::intValue).sum());
        return m;
    }

    private static String nullSafe(Object o) {
        return o == null ? "" : o.toString();
    }

    private static int nullSafeInt(Integer i) {
        return i == null ? 0 : i;
    }

    // ============ Helpers: copy spec sang entity ============
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

    // ============ Inner DTOs ============

    /**
     * Bộ cửa trích xuất từ Order (đã có ProductionSpec).
     */
    @lombok.Data
    public static class DoorEntry {
        private String orderId;
        private String orderCode;
        private String itemCode;
        private DoorLocation location;
        private String locationDetail;
        private DoorSystem doorSystem;
        private DoorType doorType;
        private List<DoorPattern> patterns = new ArrayList<>();
        private String colorName;
        private GlassType glassType;
        /** Số bộ cùng quy cách này trong dòng sản phẩm. */
        private int quantity = 1;
        private ProductionSpec spec;
        private String notes;
    }

    /**
     * Khóa nhóm quy cách SX — dùng làm key trong Map.
     * Hai DoorEntry có cùng SpecKey ⇒ cùng nhóm.
     */
    @lombok.Data
    public static class SpecKey {
        private DoorSystem doorSystem;
        private DoorType doorType;
        private String pattern;       // ghép nhiều pattern bằng "+"
        private String colorName;
        private GlassType glassType;
        private Integer wallThickness;
        private Integer leafWidth;
        private Integer leafHeight;
        private Integer verticalFrame;
        private Integer horizontalFrame;
        private Integer heightFrame;
        private Integer verticalTrim;
        private Integer horizontalTrim;
        private Integer antiGapTrimQuantity;
    }

    /**
     * Một nhóm sản xuất — gồm nhiều bộ cùng quy cách.
     */
    @lombok.Data
    public static class ProductionGroup {
        private int stt;
        private SpecKey specKey;
        private List<DoorEntry> doors = new ArrayList<>();

        /** Tổng số bộ (= sum quantity của các DoorEntry). */
        public int totalQuantity() {
            return doors.stream().mapToInt(DoorEntry::getQuantity).sum();
        }
    }

    /**
     * Báo cáo tổng thể.
     */
    @lombok.Data
    public static class ProductionOrderReport {
        private int sourceOrderCount;
        private int totalDoorCount;
        private List<ProductionGroup> groups = new ArrayList<>();
        private MaterialSummary materialSummary;
    }

    /**
     * Tổng hợp vật tư.
     */
    @lombok.Data
    public static class MaterialSummary {
        private int totalBo;
        private int totalLeaves;
        private Map<Integer, Integer> verticalFrameQty;     // key = mm
        private Map<Integer, Integer> horizontalFrameQty;
        private Map<Integer, Integer> heightFrameQty;
        private Map<Integer, Integer> verticalTrimQty;
        private Map<Integer, Integer> horizontalTrimQty;
        private Map<Integer, Integer> antiGapTrimQty;
        // Tổng từng loại
        private int totalVerticalFrame;
        private int totalHorizontalFrame;
        private int totalHeightFrame;
        private int totalVerticalTrim;
        private int totalHorizontalTrim;
        private int totalAntiGapTrim;
    }
}