package com.vietduc.plastic.model;

/**
 * Hệ cửa (hệ khung nhôm)
 */
public enum DoorSystem {
    HE_40("Hệ 40"),
    HE_45("Hệ 45");

    private final String displayName;

    DoorSystem(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}