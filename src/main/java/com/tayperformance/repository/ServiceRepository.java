package com.tayperformance.repository;

import com.tayperformance.entity.DetailService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<DetailService, Long> {
    List<DetailService> findAllByActiveTrueOrderByNameAsc();
}
