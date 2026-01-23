package com.tayperformance.dto.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerDto {

    private Long id;
    private String firstName;
    private String phone;
    private boolean active;
}