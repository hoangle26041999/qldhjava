package com.vietduc.plastic.service;

import com.vietduc.plastic.model.OrderStatusConfig;
import com.vietduc.plastic.repository.OrderStatusConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý danh mục trạng thái đơn hàng.
 */
@Service
@RequiredArgsConstructor
public class OrderStatusConfigService {

    private final OrderStatusConfigRepository repo;

    /** Trả về danh sách đang active (dùng cho dropdown). */
    public List<OrderStatusConfig> getActive() {
        return repo.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public List<OrderStatusConfig> getAll() {
        return repo.findAllByOrderByDisplayOrderAscNameAsc();
    }

    public Optional<OrderStatusConfig> getById(String id) {
        return repo.findById(id);
    }

    public Optional<OrderStatusConfig> getByCode(String code) {
        return repo.findByCode(code);
    }

    public OrderStatusConfig create(OrderStatusConfig cfg) {
        LocalDateTime now = LocalDateTime.now();
        if (cfg.getCode() == null || cfg.getCode().isBlank()) {
            throw new RuntimeException("Mã trạng thái không được để trống");
        }
        String code = cfg.getCode().trim().toUpperCase();
        if (repo.findByCode(code).isPresent()) {
            throw new RuntimeException("Mã trạng thái '" + code + "' đã tồn tại");
        }
        cfg.setCode(code);
        if (cfg.getName() == null || cfg.getName().isBlank()) {
            throw new RuntimeException("Tên trạng thái không được để trống");
        }
        if (cfg.getColor() == null || cfg.getColor().isBlank()) cfg.setColor("secondary");
        if (cfg.getActive() == null) cfg.setActive(true);
        if (cfg.getCompleted() == null) cfg.setCompleted(false);
        if (cfg.getDisplayOrder() == null) cfg.setDisplayOrder(0);
        cfg.setCreatedAt(now);
        cfg.setUpdatedAt(now);
        return repo.save(cfg);
    }

    public OrderStatusConfig update(String id, OrderStatusConfig cfg) {
        OrderStatusConfig existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trạng thái: " + id));
        if (cfg.getName() == null || cfg.getName().isBlank()) {
            throw new RuntimeException("Tên trạng thái không được để trống");
        }
        // Không cho phép đổi code nếu khác code ban đầu
        existing.setName(cfg.getName());
        existing.setColor(cfg.getColor() != null && !cfg.getColor().isBlank() ? cfg.getColor() : "secondary");
        existing.setActive(cfg.getActive() != null ? cfg.getActive() : true);
        existing.setCompleted(cfg.getCompleted() != null ? cfg.getCompleted() : false);
        existing.setDisplayOrder(cfg.getDisplayOrder() != null ? cfg.getDisplayOrder() : 0);
        existing.setDescription(cfg.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }
}
