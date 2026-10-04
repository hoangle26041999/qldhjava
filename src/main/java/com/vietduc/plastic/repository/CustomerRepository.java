package com.vietduc.plastic.repository;

import com.vietduc.plastic.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByCustomerCode(String customerCode);
    List<Customer> findByNameContainingIgnoreCase(String keyword);
    long count();
}
