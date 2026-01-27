package com.tayperformance.service.auth;

import com.tayperformance.dto.auth.RegisterRequest;
import com.tayperformance.entity.Role;
import com.tayperformance.entity.User;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Gebruiker bestaat al");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Ongeldige rol. Gebruik ADMIN of STAFF.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // ✅ belangrijk
        user.setRole(role);
        user.setActive(true);

        return userRepository.save(user);
    }
}
