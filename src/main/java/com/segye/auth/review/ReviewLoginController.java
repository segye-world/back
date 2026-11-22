package com.segye.auth.review;

import com.segye.auth.dto.AuthDtos;
import com.segye.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스토어 심사용 로그인. dev-login 과 달리 prod 에서도 켤 수 있다(앱 업데이트마다 심사를 받기 때문).
 * app.auth.review-login.enabled=true 일 때만 빈이 만들어지고, 꺼져 있으면 경로가 404 다.
 */
@ConditionalOnProperty(name = "app.auth.review-login.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/v1/auth")
public class ReviewLoginController {

    private final ReviewLoginService reviewLoginService;

    public ReviewLoginController(ReviewLoginService reviewLoginService) {
        this.reviewLoginService = reviewLoginService;
    }

    @PostMapping("/review-login")
    public ApiResponse<AuthDtos.TokenResponse> login(@RequestBody @Valid AuthDtos.ReviewLoginRequest req) {
        return ApiResponse.ok(reviewLoginService.login(req.loginId(), req.password()));
    }
}
