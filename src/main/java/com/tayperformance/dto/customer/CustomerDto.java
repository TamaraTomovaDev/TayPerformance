package com.tayperformance.dto.customer;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class CustomerDto {

    private Long id;
    private String phone;
    private String firstName;
    private String lastName;
    private String displayName;
    private boolean active;
    private OffsetDateTime createdAt;
}