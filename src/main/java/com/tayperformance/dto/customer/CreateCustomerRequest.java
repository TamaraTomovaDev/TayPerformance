package com.tayperformance.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    @NotBlank(message = "Telefoonnummer is verplicht")
    private String phone;

    private String firstName;
    private String lastName;
}