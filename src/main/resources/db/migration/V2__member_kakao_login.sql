-- 이메일·비밀번호 로그인을 카카오 로그인으로 전환 (이슈 #18)
--
-- 회원은 (provider, provider_id) 로 식별하고, email·password 컬럼은 지웁니다.
-- 운영 DB 가 생기기 전이라 기존 행은 로컬·테스트 DB 의 목 데이터뿐입니다. 이 계정들은 카카오
-- 회원번호가 없으므로 DEV 계정으로 남기고, dev-login 으로만 로그인할 수 있습니다.

ALTER TABLE member ADD COLUMN provider VARCHAR(20);
ALTER TABLE member ADD COLUMN provider_id VARCHAR(64);
ALTER TABLE member ADD COLUMN nickname VARCHAR(100);

UPDATE member
SET provider    = 'DEV',
    provider_id = 'legacy-' || id,
    nickname    = split_part(email, '@', 1);

ALTER TABLE member ALTER COLUMN provider SET NOT NULL;
ALTER TABLE member ALTER COLUMN provider_id SET NOT NULL;
ALTER TABLE member ADD CONSTRAINT member_provider_check CHECK (provider IN ('KAKAO', 'DEV'));
ALTER TABLE member ADD CONSTRAINT uk_member_provider UNIQUE (provider, provider_id);

-- 컬럼을 지우면 email 의 유니크 제약(idx_member_email)도 함께 사라진다.
ALTER TABLE member DROP COLUMN email;
ALTER TABLE member DROP COLUMN password;
