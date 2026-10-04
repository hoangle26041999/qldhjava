package com.vietduc.plastic.repository;

import com.vietduc.plastic.model.OrderStatusConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusConfigRepository extends MongoRepository<OrderStatusConfig, String> {
    List<OrderStatusConfig> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<OrderStatusConfig> findAllByOrderByDisplayOrderAscNameAsc();
    Optional<OrderStatusConfig> findByCode(String code);
}
