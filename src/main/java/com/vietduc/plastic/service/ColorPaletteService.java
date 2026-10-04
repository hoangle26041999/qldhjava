package com.vietduc.plastic.service;

import com.vietduc.plastic.model.ColorPalette;
import com.vietduc.plastic.repository.ColorPaletteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ColorPaletteService {

    private final ColorPaletteRepository repo;

    public List<ColorPalette> getAll() {
        return repo.findAllByOrderByDisplayOrderAscCodeAsc();
    }

    public List<ColorPalette> getActive() {
        return repo.findByActiveTrueOrderByDisplayOrderAscCodeAsc();
    }

    public Optional<ColorPalette> getById(String id) {
        return repo.findById(id);
    }

    public Optional<ColorPalette> getByCode(String code) {
        return repo.findByCode(code);
    }

    public ColorPalette create(ColorPalette c) {
        LocalDateTime now = LocalDateTime.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        if (c.getActive() == null) c.setActive(true);
        if (c.getDisplayOrder() == null) c.setDisplayOrder(0);
        if (c.getHexColor() == null || c.getHexColor().isBlank()) c.setHexColor("#cccccc");
        return repo.save(c);
    }

    public ColorPalette update(String id, ColorPalette c) {
        ColorPalette existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu: " + id));
        existing.setCode(c.getCode());
        existing.setName(c.getName());
        existing.setHexColor(c.getHexColor());
        existing.setGroupName(c.getGroupName());
        existing.setNotes(c.getNotes());
        existing.setActive(c.getActive() != null ? c.getActive() : true);
        existing.setDisplayOrder(c.getDisplayOrder() != null ? c.getDisplayOrder() : 0);
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    /**
     * Khởi tạo 21 màu mặc định V01..V21 nếu collection trống.
     * Gọi từ ApplicationReadyEvent hoặc khi vào trang admin lần đầu.
     */
    public void seedDefaultsIfEmpty() {
        if (repo.count() > 0) return;
        String[][] defaults = {
            // code,    name,                hex,       group
            {"V01", "Trắng sứ",            "#f5f1e8", "Trơn"},
            {"V02", "Vân gỗ nâu nhạt",     "#d4b896", "Vân gỗ"},
            {"V03", "Vân gỗ nâu đậm",      "#8b5a2b", "Vân gỗ"},
            {"V04", "Vân gỗ vàng",         "#c89968", "Vân gỗ"},
            {"V05", "Xám đá",              "#8a8a8a", "Vân đá"},
            {"V06", "Xám xi măng",         "#a8a39a", "Vân đá"},
            {"V07", "Nâu ó chó",           "#7a4a2a", "Vân gỗ"},
            {"V08", "Gỗ ó chó sáng",       "#b07a4a", "Vân gỗ"},
            {"V09", "Gỗ sồi",              "#c19a6b", "Vân gỗ"},
            {"V10", "Gỗ hương",            "#6b3a1a", "Vân gỗ"},
            {"V11", "Gỗ căm xe",           "#5c3a20", "Vân gỗ"},
            {"V12", "Xám bạc",             "#b8b8b8", "Trơn"},
            {"V13", "Đen bóng",            "#2b2b2b", "Trơn"},
            {"V14", "Kem",                 "#f0e4cc", "Trơn"},
            {"V15", "Xanh dương",          "#4a6fa5", "Đặc biệt"},
            {"V16", "Xanh ngọc",           "#4ca89a", "Đặc biệt"},
            {"V17", "Đỏ đô",              "#8b2a2a", "Đặc biệt"},
            {"V18", "Vàng kem",            "#e8c97a", "Trơn"},
            {"V19", "Vân đá cẩm thạch",    "#dcd6c8", "Vân đá"},
            {"V20", "Vân đá xám đậm",      "#5a5a5a", "Vân đá"},
            {"V21", "Vân gỗ ó chó cánh gián", "#4a2a18", "Vân gỗ"}
        };
        int order = 1;
        for (String[] d : defaults) {
            ColorPalette c = new ColorPalette();
            c.setCode(d[0]);
            c.setName(d[1]);
            c.setHexColor(d[2]);
            c.setGroupName(d[3]);
            c.setDisplayOrder(order++);
            c.setActive(true);
            create(c);
        }
    }
}