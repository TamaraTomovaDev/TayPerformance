package com.tayperformance.controller.publicapi;

import com.tayperformance.config.JwtProvider;
import com.tayperformance.dto.auth.CreateUserRequest;
import com.tayperformance.dto.auth.JwtResponse;
import com.tayperformance.dto.auth.LoginRequest;
import com.tayperformance.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public JwtResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = jwtProvider.generateToken(request.getUsername());
        return new JwtResponse(token);
    }

    /**
     * Registratie is eigenlijk INTERNAL (ADMIN) in veel projecten.
     * Maar als je dit zo wil laten: ok.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Object register(@Valid @RequestBody CreateUserRequest request) {
        var user = authService.register(request);
        return java.util.Map.of("message", "Gebruiker aangemaakt", "username", user.getUsername());
    }
}
