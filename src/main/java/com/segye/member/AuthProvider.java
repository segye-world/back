package com.segye.member;

public enum AuthProvider {
    KAKAO,
    // 카카오 회원번호가 없는 계정(로컬 목 데이터, Flyway V2 이전의 이메일 계정). dev-login 으로만 로그인할 수 있다.
    DEV,
    // 스토어 심사용 계정. review-login(심사용 아이디·비밀번호)으로만 로그인할 수 있다.
    REVIEW
}
