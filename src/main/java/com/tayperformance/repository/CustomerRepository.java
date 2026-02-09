package com.tayperformance.repository;

import com.tayperformance.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByPhoneAndActiveTrue(String phone);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndActiveTrue(String phone);

    Page<Customer> findAllByActiveTrueOrderByFirstNameAsc(Pageable pageable);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.active = true
          AND (
               LOWER(c.phone) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
          )
        ORDER BY c.firstName ASC, c.lastName ASC
    """)
    Page<Customer> searchActive(@Param("term") String term, Pageable pageable);
}
