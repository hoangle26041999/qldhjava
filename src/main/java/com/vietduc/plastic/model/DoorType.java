package com.vietduc.plastic.model;

/**
 * Loại cửa
 */
public enum DoorType {
    CUA_PHONG("Cửa phòng"),
    CUA_CHINH("Cửa chính"),
    CUA_WC("Cửa WC"),
    CUA_SO("Cửa sổ"),
    CUA_BON_GIO("Cửa bốn gió"),
    CUA_DOI("Cửa đôi"),
    CUA_DON("Cửa đơn");

    private final String displayName;

    DoorType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}