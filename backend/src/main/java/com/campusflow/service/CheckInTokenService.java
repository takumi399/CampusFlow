package com.campusflow.service;

import com.campusflow.common.ApiException;
import com.campusflow.config.CheckInProperties;
import com.campusflow.dto.CheckInTokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class CheckInTokenService {

    private final CheckInProperties properties;
    private final SecretKey key;

    public CheckInTokenService(CheckInProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public CheckInTokenResponse generate(Long eventId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.tokenTtlMinutes()));
        String token = Jwts.builder()
                .subject(String.valueOf(eventId))
                .claim("type", "CHECK_IN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new CheckInTokenResponse(
                token,
                LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
        );
    }

    public Long parseEventId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"CHECK_IN".equals(claims.get("type", String.class))) {
                throw new IllegalArgumentException("wrong token type");
            }
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_CHECK_IN_TOKEN", "签到二维码已过期或无效");
        }
    }
}
