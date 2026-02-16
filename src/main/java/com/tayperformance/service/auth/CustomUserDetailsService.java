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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security gebruikt dit bij login + bij JWT requests (via filter) om user te laden.
     * Username = email (jij gebruikt dit als identifier).
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username is leeg");
        }

        String normalized = username.trim().toLowerCase();

        User user = userRepository.findByUsernameIgnoreCase(normalized)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", normalized);
                    return new UsernameNotFoundException("Gebruiker niet gevonden: " + normalized);
                });

        // authorities: ROLE_ADMIN / ROLE_STAFF
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isActive())          // gedeactiveerd = kan niet loginnen
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(authorities)
                .build();
    }
}
