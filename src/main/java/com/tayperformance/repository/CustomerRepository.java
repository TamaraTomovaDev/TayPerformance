package com.tayperformance.repository;

import com.tayperformance.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByPhoneAndActiveTrue(String phone);

    List<Customer> findAllByActiveTrueOrderByFirstNameAsc();
}
