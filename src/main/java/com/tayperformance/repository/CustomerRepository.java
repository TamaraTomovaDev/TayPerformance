package com.tayperformance.repository;

import com.tayperformance.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneAndActiveTrue(String phone);

    List<Customer> findAllByActiveTrueOrderByFirstNameAsc();
}