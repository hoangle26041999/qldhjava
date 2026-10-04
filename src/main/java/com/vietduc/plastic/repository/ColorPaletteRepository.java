package com.vietduc.plastic.repository;

import com.vietduc.plastic.model.ColorPalette;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColorPaletteRepository extends MongoRepository<ColorPalette, String> {
    List<ColorPalette> findByActiveTrueOrderByDisplayOrderAscCodeAsc();
    List<ColorPalette> findAllByOrderByDisplayOrderAscCodeAsc();
    Optional<ColorPalette> findByCode(String code);
}