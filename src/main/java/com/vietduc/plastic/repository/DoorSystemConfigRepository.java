package com.vietduc.plastic.repository;

import com.vietduc.plastic.model.DoorSystemConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoorSystemConfigRepository extends MongoRepository<DoorSystemConfig, String> {
    List<DoorSystemConfig> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<DoorSystemConfig> findAllByOrderByDisplayOrderAscNameAsc();
    Optional<DoorSystemConfig> findByName(String name);
    Optional<DoorSystemConfig> findFirstByIsDefaultTrue();
}
