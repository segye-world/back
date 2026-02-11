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
    public ApiResponse<Void> signUp(@RequestBody @Valid AuthDtos.SignUpRequest req) {
        authService.signUp(req.getEmail(), req.getPassword());
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.TokenResponse> login(@RequestBody @Valid AuthDtos.LoginRequest req) {
        String token = authService.login(req.getEmail(), req.getPassword());
        return ApiResponse.ok(new AuthDtos.TokenResponse(token));
    }
}
