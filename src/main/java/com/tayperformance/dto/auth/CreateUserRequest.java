package com.tayperformance.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank @Email private String username;
    @NotBlank private String password;
    @NotBlank private String role; // ADMIN or STAFF
}
