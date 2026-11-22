-- 스토어 심사용 계정을 위한 REVIEW 로그인 수단 추가 (이슈 #18)
-- 심사자는 카카오 계정 없이 심사용 아이디·비밀번호로 로그인하고, 회원은 (REVIEW, 'reviewer') 하나로 고정한다.

ALTER TABLE member DROP CONSTRAINT member_provider_check;
ALTER TABLE member ADD CONSTRAINT member_provider_check CHECK (provider IN ('KAKAO', 'DEV', 'REVIEW'));
