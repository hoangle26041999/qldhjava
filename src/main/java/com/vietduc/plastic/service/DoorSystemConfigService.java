package com.vietduc.plastic.service;

import com.vietduc.plastic.model.DoorSystemConfig;
import com.vietduc.plastic.repository.DoorSystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý bảng Dòng cánh (hệ cửa: 40, 45, 50, ...).
 */
@Service
@RequiredArgsConstructor
public class DoorSystemConfigService {

    private final DoorSystemConfigRepository repo;

    /** Trả về danh sách đang active (dùng cho dropdown). */
    public List<DoorSystemConfig> getActive() {
        return repo.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public List<DoorSystemConfig> getAll() {
        return repo.findAllByOrderByDisplayOrderAscNameAsc();
    }

    public Optional<DoorSystemConfig> getById(String id) {
        return repo.findById(id);
    }

    /** Lấy hệ mặc định; nếu chưa có thì lấy cái đầu tiên; nếu rỗng thì trả null. */
    public DoorSystemConfig getDefault() {
        Optional<DoorSystemConfig> def = repo.findFirstByIsDefaultTrue();
        if (def.isPresent()) return def.get();
        List<DoorSystemConfig> all = getActive();
        return all.isEmpty() ? null : all.get(0);
    }

    public DoorSystemConfig create(DoorSystemConfig cfg) {
        LocalDateTime now = LocalDateTime.now();
        // Nếu set default = true, bỏ default của các cái khác
        if (Boolean.TRUE.equals(cfg.getIsDefault())) {
            clearDefault();
        }
        cfg.setCreatedAt(now);
        cfg.setUpdatedAt(now);
        if (cfg.getActive() == null) cfg.setActive(true);
        if (cfg.getDisplayOrder() == null) cfg.setDisplayOrder(0);
        return repo.save(cfg);
    }

    public DoorSystemConfig update(String id, DoorSystemConfig cfg) {
        DoorSystemConfig existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hệ cửa: " + id));
        if (Boolean.TRUE.equals(cfg.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            clearDefault();
        }
        existing.setName(cfg.getName());
        existing.setDescription(cfg.getDescription());
        existing.setIsDefault(cfg.getIsDefault() != null ? cfg.getIsDefault() : false);
        existing.setActive(cfg.getActive() != null ? cfg.getActive() : true);
        existing.setDisplayOrder(cfg.getDisplayOrder() != null ? cfg.getDisplayOrder() : 0);
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    private void clearDefault() {
        repo.findFirstByIsDefaultTrue().ifPresent(d -> {
            d.setIsDefault(false);
            d.setUpdatedAt(LocalDateTime.now());
            repo.save(d);
        });
    }
}
