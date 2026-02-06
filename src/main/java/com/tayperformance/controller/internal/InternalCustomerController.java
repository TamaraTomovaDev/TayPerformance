package com.tayperformance.controller.internal;

import com.tayperformance.dto.customer.CreateCustomerRequest;
import com.tayperformance.dto.customer.CustomerResponse;
import com.tayperformance.dto.customer.UpdateCustomerRequest;
import com.tayperformance.service.customer.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * Interne controller voor klantbeheer (CRUD + zoeken).
 */
@RestController
@RequestMapping("/internal/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerService service;

    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest req) {
        return service.create(req);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @PatchMapping("/{id}")
    public CustomerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest req
    ) {
        return service.update(id, req);
    }

    // ============================================================
    // ACTIVE / INACTIVE
    // ============================================================

    @PostMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }

    @PostMapping("/{id}/reactivate")
    public void reactivate(@PathVariable Long id) {
        service.reactivate(id);
    }

    // ============================================================
    // READ
    // ============================================================

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/by-phone")
    public CustomerResponse getByPhone(@RequestParam String phone) {
        return service.getByPhone(phone);
    }

    // ============================================================
    // LIST & SEARCH
    // ============================================================

    @GetMapping
    public Page<CustomerResponse> list(Pageable pageable) {
        return service.search(null, pageable); // lijst van alle actieve klanten
    }

    @GetMapping("/search")
    public Page<CustomerResponse> search(
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return service.search(q, pageable);
    }

    // ============================================================
    // GDPR DELETE
    // ============================================================

    @DeleteMapping("/{id}")
    public void hardDelete(@PathVariable Long id) {
        service.hardDelete(id);
    }
}