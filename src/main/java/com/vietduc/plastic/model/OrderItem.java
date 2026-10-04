package com.vietduc.plastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    // Tên sản phẩm
    private String productName;
    
    // Mã sản phẩm
    private String productCode;
    
    // Số lượng
    private int quantity;
    
    // Đơn giá
    private BigDecimal unitPrice;
    
    // Thành tiền
    private BigDecimal subtotal;
}
