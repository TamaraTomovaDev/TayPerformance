package com.tayperformance.dto.settings;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record GarageSettingsResponse(
        Long id,
        String garageName,
        String address,
        String phone,
        String kvkNumber,
        String logoUrl,
        String templateConfirmation,
        String templateReady,
        OffsetDateTime updatedAt
) {}
