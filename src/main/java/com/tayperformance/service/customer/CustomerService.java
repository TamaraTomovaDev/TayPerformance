package com.tayperformance.service.customer;

import com.tayperformance.dto.customer.CreateCustomerRequest;
import com.tayperformance.dto.customer.CustomerResponse;
import com.tayperformance.dto.customer.UpdateCustomerRequest;
import com.tayperformance.entity.Customer;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.CustomerMapper;
import com.tayperformance.repository.AppointmentRepository;
import com.tayperformance.repository.CustomerRepository;
import com.tayperformance.util.PhoneNumberHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service voor klantbeheer.
 * Handles: CRUD, zoeken, loyaliteit, GDPR compliance.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final AppointmentRepository appointmentRepo;

    @Value("${tay.phone.default-country:BE}")
    private String defaultCountry;

    @Value("${tay.loyalty.min-completed:4}")
    private int loyaltyThreshold;

    // ============================================================
    // CREATE
    // ============================================================

    public CustomerResponse create(CreateCustomerRequest req) {
        String phone = normalizePhone(req.getPhone());

        if (customerRepo.existsByPhone(phone)) {
            throw new BadRequestException("Telefoonnummer bestaat al");
        }

        Customer customer = Customer.builder()
                .phone(phone)
                .firstName(trim(req.getFirstName()))
                .lastName(trim(req.getLastName()))
                .active(true)
                .build();

        customer = customerRepo.save(customer);

        log.info("Created customer {}", customer.getId());
        return CustomerMapper.toResponse(customer);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public CustomerResponse update(Long id, UpdateCustomerRequest req) {
        Customer c = load(id);

        if (req.getFirstName() != null) c.setFirstName(trim(req.getFirstName()));
        if (req.getLastName() != null) c.setLastName(trim(req.getLastName()));

        c = customerRepo.save(c);

        log.info("Updated customer {}", id);
        return CustomerMapper.toResponse(c);
    }

    // ============================================================
    // ACTIVE / INACTIVE
    // ============================================================

    public void deactivate(Long id) {
        Customer c = load(id);
        if (!c.isActive()) return;

        c.setActive(false);
        customerRepo.save(c);

        log.info("Deactivated customer {}", id);
    }

    public void reactivate(Long id) {
        Customer c = load(id);
        if (c.isActive()) return;

        c.setActive(true);
        customerRepo.save(c);

        log.info("Reactivated customer {}", id);
    }

    // ============================================================
    // READ
    // ============================================================

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return CustomerMapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getByPhone(String phone) {
        String normalized = normalizePhone(phone);

        Customer c = customerRepo.findByPhoneAndActiveTrue(normalized)
                .orElseThrow(() -> new NotFoundException("Klant niet gevonden"));

        return CustomerMapper.toResponse(c);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return customerRepo.findAllByActiveTrueOrderByFirstNameAsc(pageable)
                    .map(CustomerMapper::toResponse);
        }

        return customerRepo.searchActive(q.trim(), pageable)
                .map(CustomerMapper::toResponse);
    }

    // ============================================================
    // LOYALTY & STATS
    // ============================================================

    @Transactional(readOnly = true)
    public boolean isLoyal(Long id) {
        return appointmentRepo.countCompletedByCustomer(id) >= loyaltyThreshold;
    }

    // ============================================================
    // GDPR
    // ============================================================

    public void hardDelete(Long id) {
        Customer c = load(id);

        if (c.isActive()) {
            throw new BadRequestException("Deactiveer klant eerst");
        }

        customerRepo.delete(c);
        log.warn("Hard-deleted customer {}", id);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Customer load(Long id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> NotFoundException.of("Customer", id));
    }

    private String normalizePhone(String phone) {
        String normalized = PhoneNumberHelper.normalize(phone, defaultCountry);
        if (normalized == null) throw new BadRequestException("Ongeldig telefoonnummer");
        return normalized;
    }

    private String trim(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}