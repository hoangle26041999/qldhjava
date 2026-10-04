package com.vietduc.plastic.model;

/**
 * Loại dòng sản phẩm trong bảng báo giá đơn hàng.
 * Dùng để phân loại các dòng trong Order.lineItems và render UI.
 */
public enum ProductCategory {
    DOOR,        // Bộ cửa (DT01, DT02...)
    ACCESSORY,   // Phụ kiện: khóa, bản lề, kính
    SERVICE      // Dịch vụ: lắp đặt, vận chuyển
}