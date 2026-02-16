package com.tayperformance.bootstrap;

import com.tayperformance.entity.Role;
import com.tayperformance.entity.User;
import com.tayperformance.repository.UserRepository;
import org.springframework.core.env.Environment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    public AdminBootstrapRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                Environment env) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        boolean enabled = Boolean.parseBoolean(env.getProperty("tay.bootstrap.enabled", "true"));
        if (!enabled) return;

        if (userRepository.count() > 0) return;

        String username = env.getProperty("tay.bootstrap.admin-username", "admin");
        String password = env.getProperty("tay.bootstrap.admin-password");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Bootstrap password ontbreekt (tay.bootstrap.admin-password).");
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
        System.out.println("✅ Bootstrap admin created: " + username);
    }
}

