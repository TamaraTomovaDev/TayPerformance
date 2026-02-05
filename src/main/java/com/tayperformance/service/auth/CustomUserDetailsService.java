package com.tayperformance.service.auth;

import com.tayperformance.entity.User;
import com.tayperformance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService voor Spring Security.
 * Laadt gebruikersgegevens uit database voor authenticatie.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Laad gebruiker op basis van username (email).
     * Gebruikt door Spring Security tijdens login.
     *
     * @throws UsernameNotFoundException als gebruiker niet bestaat
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("Gebruiker niet gevonden: " + username);
                });

        // Check of account actief is
        if (!user.isActive()) {
            log.warn("Attempted login with deactivated account: {}", username);
            // Spring Security zal dit interpreteren als "account disabled"
        }

        log.debug("User loaded: {} with role {}", user.getUsername(), user.getRole());

        return buildUserDetails(user);
    }

    /**
     * Converteer domain User naar Spring Security UserDetails.
     */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isActive())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .build();
    }
}