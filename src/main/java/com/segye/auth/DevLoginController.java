package com.segye.auth;

import com.segye.auth.dto.AuthDtos;
import com.segye.common.ApiResponse;
import com.segye.member.MemberRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카카오 없이 memberId 만으로 토큰을 받는 개발용 로그인. 로컬 목 데이터 계정(DEV)이나 Swagger 테스트에 쓴다.
 *
 * 누구로든 로그인할 수 있는 경로이므로 두 조건을 모두 만족할 때만 빈이 만들어진다.
 * - prod 프로필이 아닐 것
 * - app.auth.dev-login-enabled=true 를 명시적으로 켤 것 (기본값 false)
 */
@Slf4j
@Profile("!prod")
@ConditionalOnProperty(name = "app.auth.dev-login-enabled", havingValue = "true")
@RestController
@RequestMapping("/api/v1/auth")
public class DevLoginController {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider tokenProvider;

    public DevLoginController(MemberRepository memberRepository, JwtTokenProvider tokenProvider) {
        this.memberRepository = memberRepository;
        this.tokenProvider = tokenProvider;
    }

    @PostConstruct
    void warnEnabled() {
        log.warn("개발용 로그인(POST /api/v1/auth/dev-login)이 켜져 있습니다. 운영 환경에서는 절대 켜지 마세요.");
    }

    @PostMapping("/dev-login")
    public ApiResponse<AuthDtos.TokenResponse> devLogin(@RequestBody @Valid AuthDtos.DevLoginRequest req) {
        Long memberId = memberRepository.findById(req.memberId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."))
                .getId();
        return ApiResponse.ok(new AuthDtos.TokenResponse(tokenProvider.createAccessToken(memberId), false));
    }
}
