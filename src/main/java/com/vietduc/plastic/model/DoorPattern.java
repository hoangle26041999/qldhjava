package com.vietduc.plastic.model;

/**
 * Mẫu cánh cửa
 */
public enum DoorPattern {
    TRON("Trơn"),
    CNC("CNC"),
    PHAO_NOI("Phào nổi"),
    CHI_NHOM("Chỉ nhôm");

    private final String displayName;

    DoorPattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}