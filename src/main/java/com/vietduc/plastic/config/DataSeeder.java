package com.vietduc.plastic.config;

import com.vietduc.plastic.model.*;
import com.vietduc.plastic.repository.*;
import com.vietduc.plastic.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Chạy 1 lần khi ứng dụng khởi động:
 *   1. Xóa toàn bộ dữ liệu đơn hàng hiện tại trong MongoDB
 *   2. Seed bảng Dòng cánh (Hệ 40, 45) nếu chưa có
 *   3. Seed bảng Phát sinh gia công nếu chưa có
 *   4. Thêm 5 đơn hàng mẫu với cấu trúc phẳng (lineItems: DOOR + ACCESSORY + SERVICE)
 *
 * Sau khi đã có dữ liệu mẫu đầy đủ, có thể XÓA file này hoặc comment @Component.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final DoorSystemConfigRepository doorSystemConfigRepository;
    private final ProcessingFeeRepository processingFeeRepository;

    private static final String[] CUSTOMER_NAMES = {
            "Nguyễn Văn An", "Trần Thị Bích Hà", "Lê Hoàng Nam",
            "Phạm Thị Thu Trang", "Hoàng Minh Đức", "Vũ Quốc Bảo",
            "Đặng Thị Lan Anh", "Bùi Minh Tuấn", "Ngô Thị Hồng Nhung",
            "Đỗ Quang Vinh"
    };

    private static final String[] ADDRESSES = {
            "Số 12, ngõ 45, phường Dịch Vọng, Cầu Giấy, Hà Nội",
            "25 Lê Lợi, quận 1, TP. Hồ Chí Minh",
            "KĐT Vinhomes Ocean Park, Gia Lâm, Hà Nội",
            "Số 8, đường Trần Phú, Ba Đình, Hà Nội",
            "Khu biệt thự Phú Mỹ Hưng, quận 7, TP.HCM",
            "Số 102, đường Lý Thường Kiệt, Đà Nẵng",
            "KĐT Times City, Hai Bà Trưng, Hà Nội"
    };

    private static final String[] NOTES = {
            "Giao hàng giờ hành chính. Liên hệ trước 1 ngày.",
            "Khách yêu cầu sơn 2 lớp, vân gỗ tự nhiên.",
            "Công trình đang thi công, cần gấp trong 2 tuần.",
            "Thanh toán 50% khi đặt, 50% khi giao.",
            "Lắp đặt tầng 3-4, cần xe nâng.",
            ""
    };

    private static final String[] COLORS = {
            "Trắng sứ", "Vân gỗ óc chó", "Vân gỗ sồi", "Xám xi măng",
            "Vàng kem", "Nâu cà phê", "Xanh navy"
    };

    private static final String[] LOCK_TYPES = {
            "Khóa tròn Inox", "Khóa tay gạt Hafele", "Khóa thẻ từ",
            "Khóa điện tử Samsung", "Khóa tròn Việt Tiệp"
    };

    private static final String[] HINGE_TYPES = {
            "BL 4T Inox", "BL 5T mạ đồng", "BL 4T mạ đồng", "BL 5T Inox"
    };

    @Override
    public void run(String... args) {
        // 0. Seed bảng Dòng cánh (nếu rỗng)
        seedDoorSystems();
        // 0b. Seed bảng Phát sinh gia công (nếu rỗng)
        seedProcessingFees();

        // 1. Xóa toàn bộ dữ liệu hiện tại
        long existing = orderRepository.count();
        if (existing > 0) {
            orderRepository.deleteAll();
            log.info("Đã xóa {} đơn hàng cũ", existing);
        } else {
            log.info("Collection rỗng, không cần xóa");
        }

        // 2. Tạo 5 đơn hàng mẫu
        Random rnd = new Random(20261003);
        for (int i = 0; i < 5; i++) {
            Order order = buildSampleOrder(rnd, i);
            try {
                Order saved = orderService.createOrder(order);
                log.info("Đã tạo đơn mẫu #{}: {} ({} dòng / {} bộ cửa)",
                        (i + 1), saved.getOrderCode(),
                        saved.getLineItems() != null ? saved.getLineItems().size() : 0,
                        saved.getDoorLineItemCount());
            } catch (Exception e) {
                log.error("Lỗi tạo đơn mẫu #{}: {}", (i + 1), e.getMessage(), e);
            }
        }

        log.info("Seed hoàn tất. Tổng đơn: {}", orderRepository.count());
    }

    /**
     * Seed bảng Dòng cánh — chỉ thêm nếu collection rỗng.
     */
    private void seedDoorSystems() {
        if (doorSystemConfigRepository.count() > 0) {
            log.info("Bảng dòng cánh đã có {} mục, bỏ qua seed", doorSystemConfigRepository.count());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        DoorSystemConfig he40 = new DoorSystemConfig();
        he40.setName("Hệ 40");
        he40.setDescription("Hệ khung nhôm 40mm — mặc định, phù hợp cửa thông phòng, cửa WC");
        he40.setIsDefault(true);
        he40.setActive(true);
        he40.setDisplayOrder(1);
        he40.setCreatedAt(now);
        he40.setUpdatedAt(now);
        doorSystemConfigRepository.save(he40);

        DoorSystemConfig he45 = new DoorSystemConfig();
        he45.setName("Hệ 45");
        he45.setDescription("Hệ khung nhôm 45mm — cửa chính, cửa ban công, chịu lực tốt hơn");
        he45.setIsDefault(false);
        he45.setActive(true);
        he45.setDisplayOrder(2);
        he45.setCreatedAt(now);
        he45.setUpdatedAt(now);
        doorSystemConfigRepository.save(he45);

        log.info("Đã seed 2 dòng cánh mặc định: Hệ 40, Hệ 45");
    }

    /**
     * Seed bảng Phát sinh gia công — chỉ thêm nếu collection rỗng.
     */
    private void seedProcessingFees() {
        LocalDateTime now = LocalDateTime.now();
        Object[][] seedData = {
            {"CNC",       "CNC",         "Cắt CNC hoa văn trên cánh cửa",       "bộ",   "300000"},
            {"GHEP_CANH", "Ghép cánh",   "Ghép cánh phụ (cánh đôi, cánh phụ)", "cánh", "250000"},
            {"CHI_SOI",   "Chỉ soi",     "Chỉ soi trang trí trên cánh",         "bộ",   "150000"},
            {"PHAO_NOI",  "Phào nổi",    "Phào nổi trang trí cánh",             "bộ",   "200000"},
            {"O_FIX",     "Ô fix",       "Ô fix cố định bên cạnh cửa",          "bộ",   "400000"},
            {"KHAC",      "Khác",        "Phát sinh khác (nhập ghi chú)",        "lần",  "100000"},
        };
        // Luôn đảm bảo tất cả mặc định đều tồn tại (idempotent — không xóa dữ liệu user đã thêm).
        int added = 0;
        for (int i = 0; i < seedData.length; i++) {
            String code = (String) seedData[i][0];
            if (processingFeeRepository.findByCode(code).isPresent()) {
                continue; // đã có, bỏ qua
            }
            ProcessingFee fee = new ProcessingFee();
            fee.setCode(code);
            fee.setName((String) seedData[i][1]);
            fee.setDescription((String) seedData[i][2]);
            fee.setUnit((String) seedData[i][3]);
            fee.setDefaultUnitPrice(new BigDecimal((String) seedData[i][4]));
            fee.setActive(true);
            fee.setDisplayOrder(i + 1);
            fee.setCreatedAt(now);
            fee.setUpdatedAt(now);
            processingFeeRepository.save(fee);
            added++;
        }
        if (added > 0) {
            log.info("Đã seed thêm {} phát sinh gia công mặc định (tổng hiện tại: {})",
                    added, processingFeeRepository.count());
        } else {
            log.info("Bảng phát sinh gia công đã đủ {} mục mặc định", processingFeeRepository.count());
        }
    }

    /**
     * Xây dựng 1 đơn hàng mẫu với cấu trúc phẳng lineItems:
     *  - 2-4 dòng DOOR
     *  - 1-2 dòng ACCESSORY (khóa, BL)
     *  - 1 dòng SERVICE (lắp đặt)
     */
    private Order buildSampleOrder(Random rnd, int idx) {
        Order order = new Order();
        String name = CUSTOMER_NAMES[rnd.nextInt(CUSTOMER_NAMES.length)];
        String phone = String.format("09%08d", 10000000 + rnd.nextInt(89999999));
        order.setCustomerName(name);
        order.setCustomerPhone(phone);
        order.setCustomerAddress(ADDRESSES[rnd.nextInt(ADDRESSES.length)]);
        order.setStatus(OrderStatus.values()[rnd.nextInt(OrderStatus.values().length)].name());
        order.setNotes(NOTES[rnd.nextInt(NOTES.length)]);
        order.setOrderDate(LocalDateTime.now().minusDays(rnd.nextInt(30)));

        // Đảm bảo khách hàng tồn tại (sẽ được tự tạo bởi OrderService nếu chưa có)

        List<OrderLineItem> lines = new ArrayList<>();

        // Lấy danh sách dòng cánh + phát sinh gia công active để random gán
        List<DoorSystemConfig> systems = doorSystemConfigRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        if (systems.isEmpty()) systems = List.of();
        List<ProcessingFee> fees = processingFeeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        if (fees.isEmpty()) fees = List.of();

        // 1. Thêm 2-4 dòng cửa
        int doorCount = 2 + rnd.nextInt(3);
        DoorType[] types = {DoorType.CUA_PHONG, DoorType.CUA_WC, DoorType.CUA_CHINH,
                DoorType.CUA_DON, DoorType.CUA_DOI};
        for (int d = 0; d < doorCount; d++) {
            OrderLineItem door = new OrderLineItem();
            door.setCategory(ProductCategory.DOOR);
            door.setProductCode("DT" + String.format("%02d", d + 1));
            door.setProductName("Bộ cửa #" + (d + 1));
            door.setDoorType(types[rnd.nextInt(types.length)]);
            door.setDoorSystem(systems.isEmpty()
                    ? (rnd.nextBoolean() ? DoorSystem.HE_40 : DoorSystem.HE_45)
                    : (rnd.nextBoolean() ? DoorSystem.HE_40 : DoorSystem.HE_45));
            door.setColorName(COLORS[rnd.nextInt(COLORS.length)]);
            door.setGlassType(rnd.nextBoolean() ? GlassType.KHONG : GlassType.ONG_KINH_NGAN);
            door.setOpeningWidth(800 + rnd.nextInt(3) * 50);
            door.setOpeningHeight(2100 + rnd.nextInt(4) * 50);
            door.setWallThickness(100 + rnd.nextInt(3) * 25);
            door.setQuantity(1 + rnd.nextInt(3));
            door.setUnit("bộ");
            door.setUnitPrice(BigDecimal.valueOf(2_500_000L + rnd.nextInt(2_000_000)));
            // Random 0-2 phát sinh gia công cho dòng cửa
            if (!fees.isEmpty() && rnd.nextBoolean()) {
                List<String> feeIds = new ArrayList<>();
                List<String> feeNames = new ArrayList<>();
                int feeCount = 1 + rnd.nextInt(2);
                java.util.Collections.shuffle(fees, rnd);
                for (int f = 0; f < Math.min(feeCount, fees.size()); f++) {
                    feeIds.add(fees.get(f).getId());
                    feeNames.add(fees.get(f).getName());
                }
                door.setProcessingFeeIds(feeIds);
                door.setProcessingFeeNames(feeNames);
            }
            lines.add(door);
        }

        // 2. Thêm 1 dòng khóa
        OrderLineItem lock = new OrderLineItem();
        lock.setCategory(ProductCategory.ACCESSORY);
        lock.setProductCode("KHOA-01");
        lock.setAccessoryType("LOCK");
        lock.setProductName("Khóa cửa");
        lock.setAccessorySpec(LOCK_TYPES[rnd.nextInt(LOCK_TYPES.length)]);
        // Tổng khóa = tổng bộ cửa
        int totalDoors = lines.stream()
                .filter(OrderLineItem::isDoor)
                .mapToInt(l -> l.getQuantity() != null ? l.getQuantity() : 1)
                .sum();
        lock.setQuantity(totalDoors);
        lock.setUnit("cái");
        lock.setUnitPrice(BigDecimal.valueOf(150_000L + rnd.nextInt(200_000)));
        lines.add(lock);

        // 3. Thêm 1 dòng bản lề
        OrderLineItem hinge = new OrderLineItem();
        hinge.setCategory(ProductCategory.ACCESSORY);
        hinge.setProductCode("BL-01");
        hinge.setAccessoryType("HINGE");
        hinge.setProductName("Bản lề");
        hinge.setAccessorySpec(HINGE_TYPES[rnd.nextInt(HINGE_TYPES.length)]);
        hinge.setQuantity(totalDoors * 3); // 3 cái/bộ
        hinge.setUnit("cái");
        hinge.setUnitPrice(BigDecimal.valueOf(30_000L + rnd.nextInt(50_000)));
        lines.add(hinge);

        // 4. Thêm 1 dòng lắp đặt
        OrderLineItem install = new OrderLineItem();
        install.setCategory(ProductCategory.SERVICE);
        install.setProductCode("DV-01");
        install.setServiceType("LAP_DAT");
        install.setProductName("Lắp đặt trọn bộ");
        install.setServiceSpec("Bao gồm công thợ + vận chuyển nội thành");
        install.setQuantity(totalDoors);
        install.setUnit("bộ");
        install.setUnitPrice(BigDecimal.valueOf(400_000L + rnd.nextInt(300_000)));
        lines.add(install);

        order.setLineItems(lines);

        // Tổng tiền = sum totalPrice
        BigDecimal total = lines.stream()
                .map(OrderLineItem::calculateTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        return order;
    }
}