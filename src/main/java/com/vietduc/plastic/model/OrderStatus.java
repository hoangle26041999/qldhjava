package com.vietduc.plastic.model;

public enum OrderStatus {
    CHO_XAC_NHAN("Chờ xác nhận", "secondary"),
    DA_XAC_NHAN("Đã xác nhận", "info"),
    DANG_SAN_XUAT("Đang sản xuất", "warning"),
    HOAN_THANH("Hoàn thành", "success"),
    DA_HUY("Đã hủy", "danger");

    private final String displayName;
    private final String bootstrapClass;

    OrderStatus(String displayName, String bootstrapClass) {
        this.displayName = displayName;
        this.bootstrapClass = bootstrapClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBootstrapClass() {
        return bootstrapClass;
    }
}
