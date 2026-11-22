package com.segye.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final Key key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long memberId) {
        return createAccessToken(memberId, null);
    }

    public String createAccessToken(Long memberId, UUID sessionId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTokenExpMinutes() * 60);

        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp));
        if (sessionId != null) {
            builder.claim("sid", sessionId.toString()).setId(UUID.randomUUID().toString());
        }
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public Long validateAndGetMemberId(String token) {
        return validate(token).memberId();
    }

    public AccessToken validate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String sessionId = claims.get("sid", String.class);
            return new AccessToken(Long.parseLong(claims.getSubject()),
                    sessionId != null ? UUID.fromString(sessionId) : null);
        } catch (JwtException | IllegalArgumentException e) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }
    }

    public record AccessToken(Long memberId, UUID sessionId) {
    }
}
