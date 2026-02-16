package com.tayperformance.controller.internalapi;

import com.tayperformance.dto.service.CreateDetailServiceRequest;
import com.tayperformance.dto.service.DetailServiceResponse;
import com.tayperformance.dto.service.UpdateDetailServiceRequest;
import com.tayperformance.entity.DetailService;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.DetailServiceMapper;
import com.tayperformance.repository.DetailServiceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/services")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class InternalServiceController {

    private final DetailServiceRepository repo;

    @GetMapping
    public List<DetailServiceResponse> list() {
        return repo.findAll().stream()
                .map(DetailServiceMapper::toResponse)
                .toList();
    }

    @PostMapping
    public DetailServiceResponse create(@Valid @RequestBody CreateDetailServiceRequest req) {
        DetailService s = DetailService.builder()
                .name(req.getName())
                .defaultMinutes(req.getDefaultMinutes())
                .basePrice(req.getBasePrice())
                .active(true)
                .build();

        return DetailServiceMapper.toResponse(repo.save(s));
    }

    @PatchMapping("/{id}")
    public DetailServiceResponse update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateDetailServiceRequest req) {
        DetailService s = repo.findById(id)
                .orElseThrow(() -> NotFoundException.of("DetailService", id));

        if (req.getName() != null) s.setName(req.getName());
        if (req.getDefaultMinutes() != null) s.setDefaultMinutes(req.getDefaultMinutes());
        if (req.getBasePrice() != null) s.setBasePrice(req.getBasePrice());
        if (req.getActive() != null) s.setActive(req.getActive());

        return DetailServiceMapper.toResponse(repo.save(s));
    }
}
