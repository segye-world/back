package com.segye.auth;

import com.segye.auth.AuthSessionRepository.Session;
import com.segye.auth.dto.AuthDtos.SessionTokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthSessionService {

    private final AuthSessionRepository repository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties properties;
    private final SecureRandom random = new SecureRandom();

    public AuthSessionService(AuthSessionRepository repository, JwtTokenProvider tokenProvider, JwtProperties properties) {
        this.repository = repository;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    public SessionTokens create(Long memberId) {
        UUID sessionId = UUID.randomUUID();
        String refreshToken = newRefreshToken();
        Instant expiry = Instant.now().plus(properties.getRefreshTokenExpDays(), ChronoUnit.DAYS);
        repository.create(new Session(sessionId, memberId, hash(refreshToken), expiry));
        return issueTokens(memberId, sessionId, refreshToken);
    }

    public Optional<SessionTokens> refresh(String refreshToken) {
        String tokenHash = hash(refreshToken);
        return repository.lockByTokenHash(tokenHash).flatMap(session -> {
            if (!session.expiresAt().isAfter(Instant.now()) || !session.currentTokenHash().equals(tokenHash)) {
                // 이미 교체된 토큰이 재사용되면 최신 토큰을 포함해 해당 세션을 폐기한다.
                repository.delete(session.id());
                return Optional.empty();
            }
            String replacement = newRefreshToken();
            repository.rotate(session.id(), hash(replacement));
            return Optional.of(issueTokens(session.memberId(), session.id(), replacement));
        });
    }

    public void logout(String refreshToken) {
        repository.lockByTokenHash(hash(refreshToken)).ifPresent(session -> repository.delete(session.id()));
    }

    private SessionTokens issueTokens(Long memberId, UUID sessionId, String refreshToken) {
        return new SessionTokens(tokenProvider.createAccessToken(memberId, sessionId), refreshToken);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
