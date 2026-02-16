package com.tayperformance.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "tay.garage")
public class GarageProperties {

    @NotBlank
    private String name;

    @NotBlank
    private String addressLine;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    private String phone;

    /**
     * Helper method voor SMS / frontend formatting
     */
    public String getFullAddress() {
        return addressLine + ", " + postalCode + " " + city;
    }
}
