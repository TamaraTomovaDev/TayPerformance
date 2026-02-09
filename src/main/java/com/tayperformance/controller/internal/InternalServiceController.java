package com.tayperformance.controller.internal;

import com.tayperformance.entity.DetailService;
import com.tayperformance.exception.NotFoundException;
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
    public List<DetailService> list() {
        return repo.findAllByActiveTrueOrderByNameAsc();
    }

    @GetMapping("/{id}")
    public DetailService get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Service niet gevonden"));
    }

    @PostMapping
    public DetailService create(@Valid @RequestBody DetailService service) {
        service.setId(null);
        service.setVersion(null);
        return repo.save(service);
    }

    @PatchMapping("/{id}")
    public DetailService update(@PathVariable Long id, @RequestBody DetailService req) {
        DetailService s = repo.findById(id).orElseThrow(() -> new NotFoundException("Service niet gevonden"));

        if (req.getName() != null) s.setName(req.getName());
        if (req.getDefaultMinutes() != null) s.setDefaultMinutes(req.getDefaultMinutes());
        if (req.getBasePrice() != null) s.setBasePrice(req.getBasePrice());
        // active toggle (optioneel)
        // s.setActive(req.isActive());

        return repo.save(s);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        DetailService s = repo.findById(id).orElseThrow(() -> new NotFoundException("Service niet gevonden"));
        repo.delete(s);
    }
}
