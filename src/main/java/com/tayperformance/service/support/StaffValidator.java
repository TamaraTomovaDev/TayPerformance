package com.tayperformance.service.support;

import com.tayperformance.entity.Role;
import com.tayperformance.entity.User;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffValidator {

    private final UserRepository userRepository;

    public User loadAndValidate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));

        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN)
            throw new BadRequestException("Alleen staff of admins kunnen aan afspraken toegewezen worden");

        if (!user.isActive())
            throw new BadRequestException("Deze medewerker is niet actief");

        return user;
    }
}