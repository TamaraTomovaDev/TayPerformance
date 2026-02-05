package com.tayperformance.service.customer;

import com.tayperformance.dto.customer.CreateCustomerRequest;
import com.tayperformance.dto.customer.CustomerDto;
import com.tayperformance.dto.customer.UpdateCustomerRequest;
import com.tayperformance.entity.Customer;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.repository.AppointmentRepository;
import com.tayperformance.repository.CustomerRepository;
import com.tayperformance.util.PhoneNumberHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service voor klantbeheer.
 * Handles: CRUD, zoeken, loyaliteit, GDPR compliance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${tay.phone.default-country:BE}")
    private String defaultCountry;

    @Value("${tay.loyalty.min-completed:4}")
    private int loyaltyThreshold;

    // ============================================================
    // CRUD OPERATIONS
    // ============================================================

    /**
     * Maak nieuwe klant aan.
     */
    @Transactional
    public CustomerDto create(CreateCustomerRequest request) {
        String phone = normalizePhoneOrThrow(request.getPhone());

        if (customerRepository.existsByPhone(phone)) {
            throw new BadRequestException("Klant met dit telefoonnummer bestaat al");
        }

        Customer customer = Customer.builder()
                .phone(phone)
                .firstName(trimOrNull(request.getFirstName()))
                .lastName(trimOrNull(request.getLastName()))
                .active(true)
                .build();

        Customer saved = customerRepository.save(customer);

        log.info("Created customer id={} phone={}", saved.getId(), saved.getPhone());

        return toDto(saved);
    }

    /**
     * Update klantgegevens.
     */
    @Transactional
    public CustomerDto update(Long id, UpdateCustomerRequest request) {
        Customer customer = loadCustomerOrThrow(id);

        if (request.getFirstName() != null) {
            customer.setFirstName(trimOrNull(request.getFirstName()));
        }
        if (request.getLastName() != null) {
            customer.setLastName(trimOrNull(request.getLastName()));
        }

        Customer saved = customerRepository.save(customer);

        log.info("Updated customer id={}", saved.getId());

        return toDto(saved);
    }

    /**
     * Soft delete klant (GDPR: recht op vergetelheid).
     * Afspraken blijven behouden voor audit trail.
     */
    @Transactional
    public void deactivate(Long id) {
        Customer customer = loadCustomerOrThrow(id);

        if (!customer.isActive()) {
            return; // idempotent
        }

        customer.setActive(false);
        customerRepository.save(customer);

        log.info("Deactivated customer id={} phone={}", customer.getId(), customer.getPhone());
    }

    /**
     * Heractiveer klant.
     */
    @Transactional
    public void reactivate(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Customer", id));

        if (customer.isActive()) {
            return; // idempotent
        }

        customer.setActive(true);
        customerRepository.save(customer);

        log.info("Reactivated customer id={}", customer.getId());
    }

    // ============================================================
    // QUERIES
    // ============================================================

    @Transactional(readOnly = true)
    public CustomerDto getById(Long id) {
        Customer customer = loadCustomerOrThrow(id);
        return toDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDto getByPhone(String phone) {
        String normalized = normalizePhoneOrThrow(phone);

        Customer customer = customerRepository.findByPhoneAndActiveTrue(normalized)
                .orElseThrow(() -> new NotFoundException(
                        "Actieve klant niet gevonden met telefoon: " + phone
                ));

        return toDto(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getAllActive() {
        return customerRepository.findAllByActiveTrueOrderByFirstNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CustomerDto> getAllActive(Pageable pageable) {
        return customerRepository.findAllByActiveTrueOrderByFirstNameAsc(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> search(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return getAllActive();
        }

        return customerRepository.searchActive(searchTerm.trim())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CustomerDto> search(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return getAllActive(pageable);
        }

        return customerRepository.searchActive(searchTerm.trim(), pageable)
                .map(this::toDto);
    }

    // ============================================================
    // ANALYTICS & LOYALTY
    // ============================================================

    /**
     * Top klanten (meeste afspraken).
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> getTopCustomers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        return customerRepository.findTopCustomers(pageable)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Loyale klanten (≥4 completed afspraken).
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> getLoyalCustomers() {
        return customerRepository.findLoyalCustomers(loyaltyThreshold)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Check of klant loyaliteitskorting verdient.
     */
    @Transactional(readOnly = true)
    public boolean isLoyalCustomer(Long customerId) {
        long completedCount = appointmentRepository.countCompletedByCustomer(customerId);
        return completedCount >= loyaltyThreshold;
    }

    /**
     * Inactieve klanten (geen afspraken sinds X maanden).
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> getInactiveSince(int months) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMonths(months);

        return customerRepository.findInactiveSince(cutoff)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Klanten met meerdere no-shows (voor blocking).
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> getProblematicCustomers(int minNoShows, int months) {
        OffsetDateTime since = OffsetDateTime.now().minusMonths(months);

        return customerRepository.findWithMultipleNoShows(since, minNoShows)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Check of klant geblokkeerd moet worden (3+ no-shows).
     */
    @Transactional(readOnly = true)
    public boolean shouldBlock(Long customerId) {
        OffsetDateTime since = OffsetDateTime.now().minusMonths(6);
        long noShowCount = appointmentRepository.countNoShowsByCustomerSince(customerId, since);
        return noShowCount >= 3;
    }

    // ============================================================
    // GDPR COMPLIANCE
    // ============================================================

    /**
     * Klanten zonder afspraken (kandidaten voor verwijdering).
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> getCustomersWithoutAppointments(int months) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMonths(months);

        return customerRepository.findWithoutAppointmentsBefore(cutoff)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Hard delete klant (GDPR compliance).
     * WAARSCHUWING: Verwijdert ook alle gerelateerde afspraken!
     */
    @Transactional
    public void hardDelete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Customer", id));

        // Extra veiligheid: check of klant al gedeactiveerd is
        if (customer.isActive()) {
            throw new BadRequestException(
                    "Klant moet eerst gedeactiveerd worden. Hard delete alleen voor GDPR compliance."
            );
        }

        // Check of klant recent afspraken heeft
        OffsetDateTime recentCutoff = OffsetDateTime.now().minusMonths(3);
        boolean hasRecentAppointments = !appointmentRepository
                .findCompletedByCustomerSince(id, recentCutoff)
                .isEmpty();

        if (hasRecentAppointments) {
            throw new BadRequestException(
                    "Klant heeft recente afspraken. Wacht minimaal 3 maanden na laatste afspraak."
            );
        }

        customerRepository.delete(customer);

        log.warn("HARD DELETED customer id={} phone={} (GDPR)", id, customer.getPhone());
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Customer loadCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Customer", id));
    }

    private String normalizePhoneOrThrow(String phone) {
        String normalized = PhoneNumberHelper.normalize(phone, defaultCountry);
        if (normalized == null) {
            throw new BadRequestException("Ongeldig telefoonnummer: " + phone);
        }
        return normalized;
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private CustomerDto toDto(Customer customer) {
        return CustomerDto.builder()
                .id(customer.getId())
                .phone(customer.getPhone())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .displayName(customer.getDisplayName())
                .active(customer.isActive())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}