package com.tayperformance.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Geen bearer -> gewoon verder (public endpoints etc.)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        if (token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Als al authenticated is, niet opnieuw zetten
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                String username = jwtProvider.extractUsername(token);
                if (username != null && !username.isBlank()) {

                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(username.trim().toLowerCase());

                    if (jwtProvider.isTokenValid(token, userDetails)) {

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }

        } catch (JwtException ex) {
            // Token ongeldig/expired/signature wrong -> NIET crashen.
            // Laat request verder gaan; SecurityConfig zal hem blocken waar nodig.
            log.debug("Invalid JWT: {}", ex.getMessage());
        } catch (Exception ex) {
            // Nooit 500 door auth filter
            log.warn("JWT filter error: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Optioneel: filter niet toepassen op public endpoints of h2-console.
     * Dit is niet verplicht, maar maakt dev/debug leuker.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Pas aan naar jouw echte routes
        return path.startsWith("/api/public/")
                || path.startsWith("/h2-console");
    }
}
