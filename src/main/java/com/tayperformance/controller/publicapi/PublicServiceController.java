package com.tayperformance.controller.publicapi;

import com.tayperformance.dto.service.DetailServiceResponse;
import com.tayperformance.mapper.DetailServiceMapper;
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
    public List<DetailServiceResponse> listActive() {
        return repo.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(DetailServiceMapper::toResponse)
                .toList();
    }
}
