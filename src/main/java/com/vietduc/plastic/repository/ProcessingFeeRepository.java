package com.vietduc.plastic.repository;

import com.vietduc.plastic.model.ProcessingFee;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingFeeRepository extends MongoRepository<ProcessingFee, String> {
    List<ProcessingFee> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<ProcessingFee> findAllByOrderByDisplayOrderAscNameAsc();
    Optional<ProcessingFee> findByCode(String code);
}
