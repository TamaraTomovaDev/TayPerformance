package com.tayperformance.mapper;

import com.tayperformance.dto.customer.CustomerResponse;
import com.tayperformance.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {}

    public static CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .phone(c.getPhone())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .displayName(c.getDisplayName())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}