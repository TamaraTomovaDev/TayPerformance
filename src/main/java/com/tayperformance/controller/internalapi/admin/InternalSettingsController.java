package com.tayperformance.controller.internalapi.admin;

import com.tayperformance.dto.settings.GarageSettingsResponse;
import com.tayperformance.dto.settings.UpdateGarageSettingsRequest;
import com.tayperformance.service.settings.GarageSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/settings")
@RequiredArgsConstructor
public class InternalSettingsController {

    private final GarageSettingsService service;

    @GetMapping
    public ResponseEntity<GarageSettingsResponse> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    public ResponseEntity<GarageSettingsResponse> update(@Valid @RequestBody UpdateGarageSettingsRequest req) {
        return ResponseEntity.ok(service.update(req));
    }
}
