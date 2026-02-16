package com.tayperformance.service.customer;

import com.tayperformance.dto.customer.CreateCustomerRequest;
import com.tayperformance.dto.customer.CustomerHistoryResponse;
import com.tayperformance.dto.customer.CustomerResponse;
import com.tayperformance.dto.customer.UpdateCustomerRequest;
import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.Customer;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.AppointmentMapper;
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

    // -------------------- CRUD --------------------

    public CustomerResponse create(CreateCustomerRequest req) {
        String phone = normalizePhone(req.getPhone());

        if (customerRepo.existsByPhone(phone)) {
            throw new BadRequestException("Telefoonnummer bestaat al");
        }

        Customer c = Customer.builder()
                .phone(phone)
                .firstName(trim(req.getFirstName()))
                .lastName(trim(req.getLastName()))
                .active(true)
                .build();

        c = customerRepo.save(c);
        log.info("Created customer id={} phone={}", c.getId(), c.getPhone());
        return CustomerMapper.toResponse(c);
    }

    public CustomerResponse update(Long id, UpdateCustomerRequest req) {
        Customer c = load(id);

        if (req.getFirstName() != null) c.setFirstName(trim(req.getFirstName()));
        if (req.getLastName() != null) c.setLastName(trim(req.getLastName()));

        c = customerRepo.save(c);
        log.info("Updated customer id={}", id);
        return CustomerMapper.toResponse(c);
    }

    public void deactivate(Long id) {
        Customer c = load(id);
        if (!c.isActive()) return;

        c.setActive(false);
        customerRepo.save(c);
        log.info("Deactivated customer id={}", id);
    }

    public void reactivate(Long id) {
        Customer c = load(id);
        if (c.isActive()) return;

        c.setActive(true);
        customerRepo.save(c);
        log.info("Reactivated customer id={}", id);
    }

    // -------------------- READ --------------------

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

    // -------------------- HISTORY --------------------

    @Transactional(readOnly = true)
    public CustomerHistoryResponse getHistory(Long customerId) {
        Customer customer = load(customerId);

        OffsetDateTime sixMonthsAgo = OffsetDateTime.now().minusMonths(6);

        List<Appointment> recent = appointmentRepo.findCompletedByCustomerSince(customerId, sixMonthsAgo);
        long recentNoShows = appointmentRepo.countNoShowsByCustomerSince(customerId, sixMonthsAgo);
        long totalCompleted = appointmentRepo.countCompletedByCustomer(customerId);

        return CustomerHistoryResponse.builder()
                .customerId(customer.getId())
                .displayName(customer.getDisplayName())
                .phone(customer.getPhone())
                .active(customer.isActive())
                .isLoyal(totalCompleted >= loyaltyThreshold)
                .totalCompleted(totalCompleted)
                .recentNoShows(recentNoShows)
                .recentAppointments(recent.stream()
                        .map(AppointmentMapper::toResponse)
                        .limit(20)
                        .toList())
                .build();
    }

    // -------------------- helper voor AppointmentService --------------------

    public Customer findOrCreate(String phone, String fullName) {
        String normalized = normalizePhone(phone);

        return customerRepo.findByPhone(normalized)
                .map(existing -> {
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        customerRepo.save(existing);
                        log.info("Reactivated existing customer id={} phone={}", existing.getId(), existing.getPhone());
                    }
                    // optioneel: naam aanvullen als leeg
                    if ((existing.getFirstName() == null || existing.getFirstName().isBlank())
                            && fullName != null && !fullName.isBlank()) {
                        String[] parts = splitName(fullName);
                        existing.setFirstName(parts[0]);
                        existing.setLastName(parts[1]);
                        customerRepo.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    String[] parts = splitName(fullName);
                    Customer c = Customer.builder()
                            .phone(normalized)
                            .firstName(parts[0])
                            .lastName(parts[1])
                            .active(true)
                            .build();
                    Customer saved = customerRepo.save(c);
                    log.info("Auto-created customer id={} phone={}", saved.getId(), saved.getPhone());
                    return saved;
                });
    }


    // -------------------- internal helpers --------------------

    private Customer load(Long id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> NotFoundException.of("Customer", id));
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Telefoonnummer is verplicht");
        }
        String normalized = PhoneNumberHelper.normalize(phone, defaultCountry);
        if (normalized == null) throw new BadRequestException("Ongeldig telefoonnummer");
        return normalized;
    }

    private String trim(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        String[] parts = fullName.trim().split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
    }
}
