package com.tayperformance.service.support;

import com.tayperformance.entity.Customer;
import com.tayperformance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerResolver {

    private final CustomerRepository customerRepository;

    public Customer resolveOrCreate(String phone, String name) {
        return customerRepository.findByPhoneAndActiveTrue(phone)
                .map(c -> {
                    if (name != null && !name.isBlank() && !name.equals(c.getFirstName())) {
                        c.setFirstName(name.trim());
                        return customerRepository.save(c);
                    }
                    return c;
                })
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .phone(phone)
                                .firstName(name != null ? name.trim() : null)
                                .active(true)
                                .build()
                ));
    }
}