package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vị trí lắp đặt bộ cửa trong công trình.
 * Dùng để KTO phân biệt các bộ cửa khác nhau trong cùng 1 đơn hàng.
 */
public enum DoorLocation {
    PHONG_NGU("Phòng ngủ"),
    PHONG_KHACH("Phòng khách"),
    BEP("Bếp"),
    WC("WC / Nhà vệ sinh"),
    BAN_CONG("Ban công / Logia"),
    CUA_CHINH("Cửa chính"),
    CUA_CONG("Cửa cổng"),
    PHONG_LAM_VIEC("Phòng làm việc"),
    PHONG_TRE_EM("Phòng trẻ em"),
    KHO("Kho / Phụ trợ"),
    KHAC("Khác");

    private final String displayName;

    DoorLocation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}