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

    @PostMapping("/signup")
    // ERD의 Member 테이블(email, password) 기준으로 회원을 생성한다.
    public ApiResponse<Void> signUp(@RequestBody @Valid AuthDtos.SignUpRequest req) {
        authService.signUp(req.getEmail(), req.getPassword());
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    // 로그인 성공 시 memberId를 subject로 가지는 JWT Access Token을 발급한다.
    public ApiResponse<AuthDtos.TokenResponse> login(@RequestBody @Valid AuthDtos.LoginRequest req) {
        String token = authService.login(req.getEmail(), req.getPassword());
        return ApiResponse.ok(new AuthDtos.TokenResponse(token));
    }
}
