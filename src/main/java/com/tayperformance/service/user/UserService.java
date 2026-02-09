package com.tayperformance.service.user;

import com.tayperformance.entity.Role;
import com.tayperformance.entity.User;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> list(Pageable pageable) {
        return userRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public void deactivateUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));

        if (!u.isActive()) return;

        u.setActive(false);
        userRepository.save(u);
        log.info("Deactivated user id={} username={}", u.getId(), u.getUsername());
    }

    public void reactivateUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));

        if (u.isActive()) return;

        u.setActive(true);
        userRepository.save(u);
        log.info("Reactivated user id={} username={}", u.getId(), u.getUsername());
    }

    public void changePassword(Long id, String newPassword) {
        validatePassword(newPassword);

        User u = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));

        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        log.info("Changed password for user id={}", u.getId());
    }

    public void changeRole(Long id, String newRole) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));

        Role role = parseRole(newRole);

        if (u.getRole() == role) return;

        u.setRole(role);
        userRepository.save(u);

        log.info("Changed role for user id={} to {}", u.getId(), role);
    }

    // ---------------- helpers ----------------

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BadRequestException("Rol is verplicht (ADMIN/STAFF)");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Ongeldige rol: " + role + " (gebruik ADMIN of STAFF)");
        }
    }

    private void validatePassword(String p) {
        if (p == null || p.isBlank()) {
            throw new BadRequestException("Wachtwoord is verplicht");
        }
        if (p.length() < 8) {
            throw new BadRequestException("Wachtwoord moet minimaal 8 karakters zijn");
        }
        // mini “strong-ish” check (optioneel)
        boolean hasUpper = p.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = p.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = p.chars().anyMatch(Character::isDigit);
        if (!(hasUpper && hasLower && hasDigit)) {
            throw new BadRequestException("Wachtwoord moet minstens 1 hoofdletter, 1 kleine letter en 1 cijfer bevatten");
        }
    }
}
