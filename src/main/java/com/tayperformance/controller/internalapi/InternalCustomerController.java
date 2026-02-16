package com.tayperformance.controller.internalapi;

import com.tayperformance.dto.customer.CustomerHistoryResponse;
import com.tayperformance.dto.customer.CustomerResponse;
import com.tayperformance.dto.customer.UpdateCustomerRequest;
import com.tayperformance.service.customer.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class InternalCustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Page<CustomerResponse> search(@RequestParam(required = false) String q, Pageable pageable) {
        return customerService.search(q, pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping("/{id}/history")
    public CustomerHistoryResponse history(@PathVariable Long id) {
        return customerService.getHistory(id);
    }

    @PatchMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest req) {
        return customerService.update(id, req);
    }

    // ✅ REST: status toggle = PATCH
    @PatchMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        customerService.deactivate(id);
    }

    @PatchMapping("/{id}/reactivate")
    public void reactivate(@PathVariable Long id) {
        customerService.reactivate(id);
    }
}
