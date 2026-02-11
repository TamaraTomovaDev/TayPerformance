package com.tayperformance.mapper;

import com.tayperformance.dto.service.DetailServiceResponse;
import com.tayperformance.entity.DetailService;

public final class DetailServiceMapper {

    private DetailServiceMapper() {}

    public static DetailServiceResponse toResponse(DetailService s) {
        if (s == null) return null;

        return DetailServiceResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .defaultMinutes(s.getDefaultMinutes())
                .basePrice(s.getBasePrice())
                .active(s.isActive())
                .build();
    }
}
