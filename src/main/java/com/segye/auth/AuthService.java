package com.segye.auth;

import com.segye.member.Member;
import com.segye.member.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(MemberRepository memberRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public void signUp(String email, String rawPassword) {
        // ERD의 Member(email) 유니크 제약 조건에 맞춰 중복 이메일을 사전에 차단한다.
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // DB에는 평문이 아닌 BCrypt 해시 비밀번호를 저장한다.
        String hashedPassword = passwordEncoder.encode(rawPassword);
        memberRepository.save(new Member(email, hashedPassword));
    }

    @Transactional(readOnly = true)
    public String login(String email, String rawPassword) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 로그인 시에는 입력 비밀번호와 저장된 해시를 매칭해서 검증한다.
        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new SecurityException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return tokenProvider.createAccessToken(member.getId());
    }
}
