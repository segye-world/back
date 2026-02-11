package com.segye.auth;

import com.segye.member.Member;
import com.segye.member.MemberRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider tokenProvider;

    public AuthService(MemberRepository memberRepository, JwtTokenProvider tokenProvider) {
        this.memberRepository = memberRepository;
        this.tokenProvider = tokenProvider;
    }

    public void signUp(String email, String rawPassword) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String hashed = encoder.encode(rawPassword);
        memberRepository.save(new Member(email, hashed));
    }

    public String login(String email, String rawPassword) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!encoder.matches(rawPassword, member.getPassword())) {
            throw new SecurityException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return tokenProvider.createAccessToken(member.getId());
    }
}
