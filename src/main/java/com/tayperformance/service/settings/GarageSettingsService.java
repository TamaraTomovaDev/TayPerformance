package com.tayperformance.service.settings;

import com.tayperformance.dto.settings.GarageSettingsResponse;
import com.tayperformance.dto.settings.UpdateGarageSettingsRequest;
import com.tayperformance.entity.GarageSettings;
import com.tayperformance.repository.GarageSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GarageSettingsService {

    private static final long SETTINGS_ID = 1L;

    private final GarageSettingsRepository repo;

    @Transactional(readOnly = true)
    public GarageSettingsResponse get() {
        GarageSettings s = repo.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Garage settings row (id=1) ontbreekt"));
        return toResponse(s);
    }

    @Transactional
    public GarageSettingsResponse update(UpdateGarageSettingsRequest req) {
        GarageSettings s = repo.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Garage settings row (id=1) ontbreekt"));

        s.setGarageName(req.garageName().trim());
        s.setAddress(req.address().trim());
        s.setPhone(req.phone() != null ? req.phone().trim() : null);
        s.setKvkNumber(req.kvkNumber() != null ? req.kvkNumber().trim() : null);
        s.setLogoUrl(req.logoUrl() != null ? req.logoUrl().trim() : null);

        s.setTemplateConfirmation(req.templateConfirmation().trim());
        s.setTemplateReady(req.templateReady().trim());

        return toResponse(repo.save(s));
    }

    private GarageSettingsResponse toResponse(GarageSettings s) {
        return GarageSettingsResponse.builder()
                .id(s.getId())
                .garageName(s.getGarageName())
                .address(s.getAddress())
                .phone(s.getPhone())
                .kvkNumber(s.getKvkNumber())
                .logoUrl(s.getLogoUrl())
                .templateConfirmation(s.getTemplateConfirmation())
                .templateReady(s.getTemplateReady())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}

