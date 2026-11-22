package com.segye.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuthSessionRepository {

    private final JdbcTemplate jdbc;

    public AuthSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record Session(UUID id, Long memberId, String currentTokenHash, Instant expiresAt) {
    }

    void create(Session session) {
        jdbc.update("""
                INSERT INTO auth_session (id, member_id, current_token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """, session.id(), session.memberId(), session.currentTokenHash(),
                session.expiresAt().atOffset(ZoneOffset.UTC));
        insertToken(session.id(), session.currentTokenHash());
    }

    Optional<Session> lockByTokenHash(String tokenHash) {
        return jdbc.query("""
                SELECT s.id, s.member_id, s.current_token_hash, s.expires_at
                FROM auth_session s
                JOIN auth_refresh_token t ON t.session_id = s.id
                WHERE t.token_hash = ?
                FOR UPDATE OF s
                """, (rs, rowNum) -> new Session(
                        rs.getObject("id", UUID.class), rs.getLong("member_id"),
                        rs.getString("current_token_hash"), rs.getObject("expires_at", OffsetDateTime.class).toInstant()),
                tokenHash).stream().findFirst();
    }

    void rotate(UUID sessionId, String newTokenHash) {
        insertToken(sessionId, newTokenHash);
        jdbc.update("UPDATE auth_session SET current_token_hash = ? WHERE id = ?", newTokenHash, sessionId);
    }

    void delete(UUID sessionId) {
        jdbc.update("DELETE FROM auth_session WHERE id = ?", sessionId);
    }

    public boolean isActive(UUID sessionId, Long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM auth_session
                    WHERE id = ? AND member_id = ? AND expires_at > CURRENT_TIMESTAMP
                )
                """, Boolean.class, sessionId, memberId));
    }

    private void insertToken(UUID sessionId, String tokenHash) {
        jdbc.update("INSERT INTO auth_refresh_token (token_hash, session_id) VALUES (?, ?)", tokenHash, sessionId);
    }
}
