package com.tayperformance.repository;

import com.tayperformance.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Zoek klant op telefoonnummer
     * (telefoon is uniek)
     */
    Optional<Customer> findByPhone(String phone);
}
