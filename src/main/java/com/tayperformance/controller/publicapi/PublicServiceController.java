// src/main/java/com/tayperformance/controller/publicapi/PublicServiceController.java
package com.tayperformance.controller.publicapi;

import com.tayperformance.entity.DetailService;
import com.tayperformance.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/services")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicServiceController {

    private final ServiceRepository serviceRepository;

    @GetMapping
    public List<DetailService> listActive() {
        return serviceRepository.findAllByActiveTrueOrderByNameAsc();
    }
}
