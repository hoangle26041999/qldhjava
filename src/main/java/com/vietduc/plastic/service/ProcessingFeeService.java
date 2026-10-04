package com.vietduc.plastic.service;

import com.vietduc.plastic.model.ProcessingFee;
import com.vietduc.plastic.repository.ProcessingFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service quản lý bảng Phát sinh gia công (CNC, GHÉP CÁNH, CHỈ SOI, ...).
 *
 * Quy tắc:
 *  - Tạo mới: tự động đặt displayOrder = max + 1 để dòng mới luôn nằm cuối.
 *  - Cập nhật: KHÔNG động vào displayOrder (giữ nguyên vị trí do user sắp xếp).
 *  - Validate: code + name bắt buộc, code không trùng (trừ chính nó khi update).
 */
@Service
@RequiredArgsConstructor
public class ProcessingFeeService {

    private final ProcessingFeeRepository repo;

    /** Lấy tất cả phát sinh đang dùng, sắp xếp theo displayOrder rồi tên. */
    public List<ProcessingFee> getActive() {
        return repo.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    /** Lấy tất cả phát sinh (gồm cả tắt), sắp xếp theo displayOrder rồi tên. */
    public List<ProcessingFee> getAll() {
        return repo.findAllByOrderByDisplayOrderAscNameAsc();
    }

    public Optional<ProcessingFee> getById(String id) {
        return repo.findById(id);
    }

    /**
     * Tạo mới phát sinh. Tự động gán displayOrder = max + 1.
     * @throws IllegalArgumentException nếu code/name rỗng hoặc code đã tồn tại.
     */
    public ProcessingFee create(ProcessingFee fee) {
        validateForCreate(fee);
        LocalDateTime now = LocalDateTime.now();
        fee.setCreatedAt(now);
        fee.setUpdatedAt(now);

        if (fee.getActive() == null) fee.setActive(true);
        if (fee.getUnit() == null || fee.getUnit().isBlank()) fee.setUnit("bộ");
        if (fee.getDefaultUnitPrice() == null) fee.setDefaultUnitPrice(BigDecimal.ZERO);

        // Tự động đặt displayOrder = max hiện tại + 1 để dòng mới luôn nằm cuối.
        // Bỏ qua null/0 để tránh dòng mới luôn lấy order=1 khi data cũ chưa có order.
        int nextOrder = repo.findAll().stream()
                .map(f -> f.getDisplayOrder() == null ? 0 : f.getDisplayOrder())
                .filter(o -> o > 0)
                .max(Integer::compare)
                .orElse(0) + 1;
        fee.setDisplayOrder(nextOrder);

        return repo.save(fee);
    }

    /**
     * Cập nhật phát sinh theo id. Không thay đổi displayOrder.
     * @throws IllegalArgumentException nếu không tìm thấy hoặc code trùng với dòng khác.
     */
    public ProcessingFee update(String id, ProcessingFee fee) {
        ProcessingFee existing = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phát sinh: " + id));
        validateForUpdate(fee, id);

        existing.setCode(fee.getCode());
        existing.setName(fee.getName());
        existing.setDescription(fee.getDescription());
        existing.setDefaultUnitPrice(fee.getDefaultUnitPrice() != null ? fee.getDefaultUnitPrice() : BigDecimal.ZERO);
        existing.setUnit(fee.getUnit() != null && !fee.getUnit().isBlank() ? fee.getUnit() : "bộ");
        existing.setActive(fee.getActive() != null ? fee.getActive() : true);
        // KHÔNG thay đổi displayOrder khi update — giữ vị trí do user sắp xếp.
        if (fee.getDisplayOrder() != null) {
            existing.setDisplayOrder(fee.getDisplayOrder());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy phát sinh: " + id);
        }
        repo.deleteById(id);
    }

    /**
     * Dọn rác: xoá tất cả document có id rỗng hoặc null.
     * Dùng để dọn dữ liệu phát sinh từ test thủ công trước đây.
     * @return số document đã xoá.
     */
    public int deleteEmptyIds() {
        List<ProcessingFee> all = repo.findAll();
        int removed = 0;
        for (ProcessingFee f : all) {
            if (f.getId() == null || f.getId().isBlank()) {
                // Dùng repository.delete(entity) để tránh lỗi deleteById(null/empty).
                repo.delete(f);
                removed++;
            }
        }
        return removed;
    }

    // ============ Validation ============
    private void validateForCreate(ProcessingFee fee) {
        if (fee.getCode() == null || fee.getCode().isBlank()) {
            throw new IllegalArgumentException("Mã (code) không được để trống.");
        }
        if (fee.getName() == null || fee.getName().isBlank()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống.");
        }
        if (repo.findByCode(fee.getCode().trim()).isPresent()) {
            throw new IllegalArgumentException("Mã \"" + fee.getCode() + "\" đã tồn tại, vui lòng chọn mã khác.");
        }
    }

    private void validateForUpdate(ProcessingFee fee, String currentId) {
        if (fee.getCode() == null || fee.getCode().isBlank()) {
            throw new IllegalArgumentException("Mã (code) không được để trống.");
        }
        if (fee.getName() == null || fee.getName().isBlank()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống.");
        }
        // Trùng code nhưng là chính nó thì OK.
        Optional<ProcessingFee> byCode = repo.findByCode(fee.getCode().trim());
        if (byCode.isPresent() && !Objects.equals(byCode.get().getId(), currentId)) {
            throw new IllegalArgumentException("Mã \"" + fee.getCode() + "\" đã được dùng cho phát sinh khác.");
        }
    }
}