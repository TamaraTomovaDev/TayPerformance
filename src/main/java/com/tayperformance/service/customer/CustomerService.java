package com.tayperformance.service.customer;

import com.tayperformance.dto.customer.CustomerDto;
import com.tayperformance.entity.Customer;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerDto getByPhone(String phone) {
        Customer customer = customerRepository.findByPhoneAndActiveTrue(phone)
                .orElseThrow(() ->
                        new NotFoundException("Klant niet gevonden met telefoon: " + phone));

        return toDto(customer);
    }

    public List<CustomerDto> getAllActiveCustomers() {
        return customerRepository.findAllByActiveTrueOrderByFirstNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deactivateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Klant niet gevonden met id: " + id));

        customer.setActive(false);
    }

    private CustomerDto toDto(Customer customer) {
        return CustomerDto.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .phone(customer.getPhone())
                .active(customer.isActive())
                .build();
    }
}
