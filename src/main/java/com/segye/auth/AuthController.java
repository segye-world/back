package com.segye.auth;

import com.segye.auth.dto.AuthDtos;
import com.segye.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthSessionService sessionService;

    public AuthController(AuthService authService, AuthSessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/kakao")
    // 카카오 토큰을 검증해 회원을 찾거나 만들고, memberId 를 subject 로 가지는 JWT 를 발급한다.
    public ApiResponse<AuthDtos.TokenResponse> loginWithKakao(@RequestBody @Valid AuthDtos.KakaoLoginRequest req) {
        return ApiResponse.ok(authService.loginWithKakao(req.accessToken()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.SessionTokens> refresh(@RequestBody @Valid AuthDtos.RefreshTokenRequest req) {
        return ApiResponse.ok(sessionService.refresh(req.refreshToken())
                .orElseThrow(() -> new SecurityException("세션이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.")));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid AuthDtos.RefreshTokenRequest req) {
        sessionService.logout(req.refreshToken());
        return ApiResponse.ok();
    }
}
