package com.tayperformance.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
@Getter
public class JwtProvider {

    private final String secret;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:43200000}") long expirationMs // default: 12h
    ) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    private Key getSigningKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        // HS256: 32 bytes minimum recommended
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 bytes for HS256. " +
                            "Tip: use something like 'devSecret12345_devSecret12345_dev!'"
            );
        }

        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username.toLowerCase())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null
                && username.equalsIgnoreCase(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
