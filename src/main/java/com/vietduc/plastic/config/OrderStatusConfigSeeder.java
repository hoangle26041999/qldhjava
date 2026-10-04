package com.vietduc.plastic.config;

import com.vietduc.plastic.model.OrderStatusConfig;
import com.vietduc.plastic.repository.OrderStatusConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seed bảng danh mục trạng thái đơn hàng lần đầu (nếu collection rỗng).
 * Mapping 1-1 với 5 giá trị enum OrderStatus đang dùng trong code.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusConfigSeeder implements CommandLineRunner {

    private final OrderStatusConfigRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            log.info("Bảng order_statuses đã có {} mục, bỏ qua seed", repo.count());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Object[][] seed = {
            // {code, name, color, displayOrder, completed, description}
            {"CHO_XAC_NHAN",   "Chờ xác nhận",   "secondary", 1, false, "Đơn mới tạo, chờ nhân viên xác nhận"},
            {"DA_XAC_NHAN",    "Đã xác nhận",    "info",      2, false, "Đã xác nhận đơn, chờ đưa vào sản xuất"},
            {"DANG_SAN_XUAT",  "Đang sản xuất",  "warning",   3, false, "Đang trong quy trình sản xuất"},
            {"HOAN_THANH",     "Hoàn thành",     "success",   4, true,  "Đã hoàn thành sản xuất"},
            {"DA_HUY",         "Đã hủy",         "danger",    5, false, "Đơn đã bị hủy"},
        };
        for (Object[] row : seed) {
            OrderStatusConfig c = new OrderStatusConfig();
            c.setCode((String) row[0]);
            c.setName((String) row[1]);
            c.setColor((String) row[2]);
            c.setDisplayOrder((Integer) row[3]);
            c.setCompleted((Boolean) row[4]);
            c.setActive(true);
            c.setDescription((String) row[5]);
            c.setCreatedAt(now);
            c.setUpdatedAt(now);
            repo.save(c);
        }
        log.info("Đã seed {} trạng thái mặc định vào bảng order_statuses", seed.length);
    }
}
