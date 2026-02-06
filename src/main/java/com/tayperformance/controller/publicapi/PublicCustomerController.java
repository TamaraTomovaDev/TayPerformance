package com.tayperformance.controller.publicapi;

import com.tayperformance.dto.customer.CustomerResponse;
import com.tayperformance.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Publieke klantendata (alleen lezen).
 */
@RestController
@RequestMapping("/public/customers")
@RequiredArgsConstructor
public class PublicCustomerController {

    private final CustomerService service;

    /**
     * Zoeken op telefoonnummer (alleen actieve klanten).
     */
    @GetMapping("/by-phone")
    public CustomerResponse getByPhone(@RequestParam String phone) {
        return service.getByPhone(phone);
    }

    /**
     * Klant ophalen op ID.
     */
    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }
}