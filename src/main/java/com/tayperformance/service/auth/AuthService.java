package com.tayperformance.service.auth;

import com.tayperformance.dto.auth.RegisterRequest;
import com.tayperformance.entity.Role;
import com.tayperformance.entity.User;
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
            throw new IllegalArgumentException("Gebruiker bestaat al");
        }

        Role role = Role.valueOf(request.getRole().toUpperCase()); // ADMIN of STAFF

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(true);

        return userRepository.save(user);
    }
}
