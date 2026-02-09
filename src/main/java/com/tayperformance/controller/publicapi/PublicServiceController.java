package com.tayperformance.controller.publicapi;

import com.tayperformance.entity.DetailService;
import com.tayperformance.repository.DetailServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/services")
@RequiredArgsConstructor
public class PublicServiceController {

    private final DetailServiceRepository repo;

    @GetMapping
    public List<DetailService> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public DetailService get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new com.tayperformance.exception.NotFoundException("Service niet gevonden"));
    }
}
