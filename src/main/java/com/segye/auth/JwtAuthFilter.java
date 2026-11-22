package com.segye.auth;

import com.segye.member.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final AuthSessionRepository sessions;

    public JwtAuthFilter(JwtTokenProvider tokenProvider, MemberRepository memberRepository,
                         AuthSessionRepository sessions) {
        this.tokenProvider = tokenProvider;
        this.memberRepository = memberRepository;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JwtTokenProvider.AccessToken access = tokenProvider.validate(token);
                Long memberId = access.memberId();
                boolean active = access.sessionId() == null
                        ? memberRepository.existsById(memberId) // 기존 앱에 발급된 JWT도 만료 전까지 허용한다.
                        : sessions.isActive(access.sessionId(), memberId);
                if (!active) {
                    throw new SecurityException("유효하지 않은 세션입니다.");
                }

                // principal에 memberId만 넣어도 MVP에는 충분
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(memberId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (SecurityException e) {
                // 무효 토큰은 미인증 상태로 통과시킨다.
                // 보호된 엔드포인트는 뒤의 인가 단계에서 401로 거절되고, permitAll 경로는 정상 동작한다.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
