package com.vietduc.plastic.repository;

import com.vietduc.plastic.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // Tìm theo mã đơn hàng
    Optional<Order> findByOrderCode(String orderCode);

    // Tìm theo số điện thoại khách hàng
    List<Order> findByCustomerPhoneContaining(String phone);

    // Tìm theo tên khách hàng
    List<Order> findByCustomerNameContainingIgnoreCase(String name);

    // Tìm theo trạng thái (code String)
    List<Order> findByStatus(String status);

    // Tìm theo khoảng ngày
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Tìm đơn hàng mới nhất để tạo mã
    Optional<Order> findTopByOrderByCreatedAtDesc();
}
