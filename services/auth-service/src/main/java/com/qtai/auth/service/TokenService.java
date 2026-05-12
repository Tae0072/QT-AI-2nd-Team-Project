package com.qtai.auth.service;

import com.qtai.auth.dto.TokenResponse;
import com.qtai.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class TokenService {

    private static final long ACCESS_TOKEN_TTL  = 30 * 60 * 1000L;  // 30분
    private static final long REFRESH_TOKEN_TTL = 14 * 24 * 60 * 60 * 1000L; // 14일

    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenResponse issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        long now = System.currentTimeMillis();
        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + ACCESS_TOKEN_TTL))
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + REFRESH_TOKEN_TTL))
                .signWith(key)
                .compact();

        log.info("Token issued: userId={}", userId);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(ACCESS_TOKEN_TTL / 1000)
                .build();
    }

    public Long validate(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token must not be blank");
        }
        try {
            var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired token");
        }
    }
}
