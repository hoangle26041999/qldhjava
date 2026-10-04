package com.vietduc.plastic.model;

/**
 * Màu cửa
 */
public enum Color {
    TRANG("Trắng"),
    VANH("Vân gỗ"),
    XAM("Xám"),
    DEN("Đen"),
    XANH("Xanh"),
    DO("Đỏ"),
    VANG("Vàng"),
    NAU("Nâu"),
    KHAC("Khác");

    private final String displayName;

    Color(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}