package com.tayperformance.service.auth;

import com.tayperformance.dto.auth.CreateUserRequest;
import com.tayperformance.entity.Role;
import com.tayperformance.entity.User;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service voor authenticatie en gebruikersbeheer.
 * Handles: registratie, wachtwoord validatie, activatie/deactivatie.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$";

    /**
     * Registreer nieuwe gebruiker (ADMIN of STAFF).
     *
     * Validaties:
     * - Unieke username
     * - Sterk wachtwoord
     * - Geldige rol
     */
    @Transactional
    public User register(CreateUserRequest request) {
        validateRegistrationRequest(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Gebruikersnaam bestaat al: " + request.getUsername());
        }

        Role role = parseRole(request.getRole());

        User user = User.builder()
                .username(request.getUsername().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .build();

        User saved = userRepository.save(user);

        log.info("Registered new user: {} with role {}", saved.getUsername(), saved.getRole());

        return saved;
    }

    /**
     * Deactiveer gebruiker (soft delete).
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Gebruiker niet gevonden"));

        if (!user.isActive()) {
            log.warn("User id={} already deactivated", userId);
            return; // idempotent
        }

        user.setActive(false);
        userRepository.save(user);

        log.info("Deactivated user: {}", user.getUsername());
    }

    /**
     * Heractiveer gebruiker.
     */
    @Transactional
    public void reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Gebruiker niet gevonden"));

        if (user.isActive()) {
            return; // idempotent
        }

        user.setActive(true);
        userRepository.save(user);

        log.info("Reactivated user: {}", user.getUsername());
    }

    /**
     * Wijzig wachtwoord.
     */
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Gebruiker niet gevonden"));

        validatePassword(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Wijzig rol van gebruiker.
     */
    @Transactional
    public void changeRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Gebruiker niet gevonden"));

        Role role = parseRole(newRole);

        if (user.getRole() == role) {
            return; // no change
        }

        user.setRole(role);
        userRepository.save(user);

        log.info("Changed role for user {} from {} to {}",
                user.getUsername(), user.getRole(), role);
    }

    // ============================================================
    // VALIDATION HELPERS
    // ============================================================

    private void validateRegistrationRequest(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BadRequestException("Gebruikersnaam is verplicht");
        }

        if (!request.getUsername().contains("@")) {
            throw new BadRequestException("Gebruikersnaam moet een geldig email adres zijn");
        }

        validatePassword(request.getPassword());

        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new BadRequestException("Rol is verplicht");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException(
                    "Wachtwoord moet minimaal " + MIN_PASSWORD_LENGTH + " karakters lang zijn"
            );
        }

        if (!password.matches(PASSWORD_PATTERN)) {
            throw new BadRequestException(
                    "Wachtwoord moet minimaal 1 hoofdletter, 1 kleine letter en 1 cijfer bevatten"
            );
        }
    }

    private Role parseRole(String roleString) {
        if (roleString == null || roleString.isBlank()) {
            throw new BadRequestException("Rol is verplicht");
        }

        try {
            return Role.valueOf(roleString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Ongeldige rol: " + roleString + ". Gebruik ADMIN of STAFF."
            );
        }
    }
}