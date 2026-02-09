package com.tayperformance.controller.publicapi;

import com.tayperformance.dto.customer.CustomerResponse;
import com.tayperformance.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/customers")
@RequiredArgsConstructor
public class PublicCustomerController {

    private final CustomerService customerService;

    @GetMapping("/by-phone")
    public CustomerResponse getByPhone(@RequestParam String phone) {
        return customerService.getByPhone(phone);
    }
}
