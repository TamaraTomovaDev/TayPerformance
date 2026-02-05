package com.tayperformance.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Gebruikersnaam is verplicht")
    private String username;

    @NotBlank(message = "Wachtwoord is verplicht")
    private String password;

    @NotBlank(message = "Rol is verplicht (ADMIN of STAFF)")
    private String role;
}