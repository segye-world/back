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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/kakao")
    // 카카오 토큰을 검증해 회원을 찾거나 만들고, memberId 를 subject 로 가지는 JWT 를 발급한다.
    public ApiResponse<AuthDtos.TokenResponse> loginWithKakao(@RequestBody @Valid AuthDtos.KakaoLoginRequest req) {
        AuthService.LoginResult result = authService.loginWithKakao(req.accessToken());
        return ApiResponse.ok(new AuthDtos.TokenResponse(result.accessToken(), result.isNewMember()));
    }
}
