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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$";

    @Transactional
    public User register(CreateUserRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) throw new BadRequestException("Username is verplicht");
        if (req.getPassword() == null || req.getPassword().isBlank()) throw new BadRequestException("Password is verplicht");

        validatePassword(req.getPassword());

        String username = req.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) throw new BadRequestException("Gebruikersnaam bestaat al: " + username);

        Role role;
        try {
            role = Role.valueOf(req.getRole().trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Ongeldige rol: " + req.getRole() + " (ADMIN of STAFF)");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered user {} role={}", saved.getUsername(), saved.getRole());
        return saved;
    }

    private void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException("Wachtwoord moet minimaal " + MIN_PASSWORD_LENGTH + " karakters lang zijn");
        }
        if (!password.matches(PASSWORD_PATTERN)) {
            throw new BadRequestException("Wachtwoord moet minimaal 1 hoofdletter, 1 kleine letter en 1 cijfer bevatten");
        }
    }
}
