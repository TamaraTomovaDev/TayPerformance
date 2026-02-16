package com.tayperformance.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGarageSettingsRequest(
        @NotBlank @Size(max = 120) String garageName,
        @NotBlank @Size(max = 255) String address,
        @Size(max = 40) String phone,
        @Size(max = 40) String kvkNumber,
        @Size(max = 255) String logoUrl,

        @NotBlank @Size(max = 4000) String templateConfirmation,
        @NotBlank @Size(max = 4000) String templateReady
) {}
