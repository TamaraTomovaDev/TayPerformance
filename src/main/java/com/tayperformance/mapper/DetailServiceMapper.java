package com.tayperformance.mapper;

import com.tayperformance.dto.service.ServiceDto;
import com.tayperformance.entity.DetailService;

public final class DetailServiceMapper {

    private DetailServiceMapper() {}

    public static ServiceDto toDto(DetailService s) {
        if (s == null) return null;

        return ServiceDto.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .minMinutes(s.getMinMinutes())
                .maxMinutes(s.getMaxMinutes())
                .defaultMinutes(s.getDefaultMinutes())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}