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

    // ----------- LIST (admin) -----------

    @Transactional(readOnly = true)
    public Page<User> list(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return userRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(q.trim(), pageable);
    }

    // ----------- CREATE (admin) -----------

    public User createUser(String username, String rawPassword, Role role) {
        if (username == null || username.isBlank()) throw new BadRequestException("Username is verplicht");
        if (rawPassword == null || rawPassword.isBlank()) throw new BadRequestException("Password is verplicht");
        if (role == null) throw new BadRequestException("Role is verplicht");

        validatePassword(rawPassword);

        String normalized = username.trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new BadRequestException("Gebruikersnaam bestaat al: " + normalized);
        }

        User user = User.builder()
                .username(normalized)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Created user id={} username={} role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return saved;
    }

    // ----------- ACTIVATE / DEACTIVATE -----------

    public void deactivateUser(Long id) {
        User u = load(id);
        if (!u.isActive()) return;

        u.setActive(false);
        userRepository.save(u);
        log.info("Deactivated user id={} username={}", u.getId(), u.getUsername());
    }

    public void reactivateUser(Long id) {
        User u = load(id);
        if (u.isActive()) return;

        u.setActive(true);
        userRepository.save(u);
        log.info("Reactivated user id={} username={}", u.getId(), u.getUsername());
    }

    // ----------- PASSWORD / ROLE -----------

    public void changePassword(Long id, String newPassword) {
        validatePassword(newPassword);

        User u = load(id);
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        log.info("Changed password for user id={}", u.getId());
    }

    public void changeRole(Long id, Role newRole) {
        if (newRole == null) throw new BadRequestException("Rol is verplicht (ADMIN/STAFF)");

        User u = load(id);
        if (u.getRole() == newRole) return;

        u.setRole(newRole);
        userRepository.save(u);
        log.info("Changed role for user id={} to {}", u.getId(), newRole);
    }

    // ----------- helpers -----------

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }

    private void validatePassword(String p) {
        if (p == null || p.isBlank()) {
            throw new BadRequestException("Wachtwoord is verplicht");
        }
        if (p.length() < 8) {
            throw new BadRequestException("Wachtwoord moet minimaal 8 karakters zijn");
        }
        boolean hasUpper = p.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = p.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = p.chars().anyMatch(Character::isDigit);
        if (!(hasUpper && hasLower && hasDigit)) {
            throw new BadRequestException("Wachtwoord moet minstens 1 hoofdletter, 1 kleine letter en 1 cijfer bevatten");
        }
    }
}
