package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Khách hàng — lưu riêng để tái sử dụng khi tạo đơn hàng.
 * <p>
 * Mỗi khách được xác định bằng số điện thoại (unique).
 * Khi tạo đơn, form tra cứu theo SĐT hoặc tên — nếu chưa có thì tự lưu mới.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class Customer {

    @Id
    private String id;

    /** Mã khách tự sinh: KH-0001, KH-0002... */
    @Indexed(unique = true)
    private String customerCode;

    /** Họ tên khách hàng. */
    private String name;

    /** Số điện thoại (khóa tra cứu chính). */
    @Indexed(unique = true)
    private String phone;

    /** Địa chỉ. */
    private String address;

    /** Ghi chú. */
    private String notes;

    /** Số đơn hàng đã tạo (đếm ngược). */
    private Integer totalOrders = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Tự sinh mã KH-XXXX theo thứ tự.
     */
    public static String generateCode(long sequence) {
        return "KH-" + String.format("%04d", sequence);
    }
}
