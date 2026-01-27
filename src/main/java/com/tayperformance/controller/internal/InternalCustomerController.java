package com.tayperformance.controller.internal;

import com.tayperformance.dto.customer.CustomerDto;
import com.tayperformance.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllActiveCustomers());
    }

    @GetMapping("/search")
    public ResponseEntity<CustomerDto> getByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(customerService.getByPhone(phone));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
