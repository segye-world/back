CREATE TABLE auth_session (
    id                   UUID        PRIMARY KEY,
    member_id            BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    current_token_hash   VARCHAR(64) NOT NULL,
    expires_at           TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auth_session_member ON auth_session (member_id);

-- 사용한 토큰의 해시도 세션이 끝날 때까지 보관해 재사용을 감지한다.
CREATE TABLE auth_refresh_token (
    token_hash VARCHAR(64) PRIMARY KEY,
    session_id UUID        NOT NULL REFERENCES auth_session (id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_refresh_token_session ON auth_refresh_token (session_id);
