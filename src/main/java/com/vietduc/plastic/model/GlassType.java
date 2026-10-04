package com.vietduc.plastic.model;

/**
 * Loại ô kính
 */
public enum GlassType {
    KHONG("Không"),
    ONG_KINH_NGAN("Ô kính ngắn"),
    ONG_KINH_DAI("Ô kính dài"),
    O_GIO("Ô gió");

    private final String displayName;

    GlassType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}