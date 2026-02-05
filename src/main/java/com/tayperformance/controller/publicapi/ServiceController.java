// src/main/java/com/tayperformance/controller/publicapi/PublicServiceController.java
package com.tayperformance.controller.publicapi;

import com.tayperformance.entity.DetailService;
import com.tayperformance.repository.DetailServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/services")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceController {

    private final DetailServiceRepository serviceRepository;

    @GetMapping
    public List<DetailService> listActive() {
        return serviceRepository.findAllByActiveTrueOrderByNameAsc();
    }
}
