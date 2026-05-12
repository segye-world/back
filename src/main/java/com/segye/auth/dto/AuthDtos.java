package com.segye.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public class AuthDtos {

    @Getter
    public static class SignUpRequest {
        @Email
        @NotBlank
        private String email;

        // 최소 길이를 제한해 너무 약한 비밀번호 입력을 방지한다.
        @NotBlank
        @Size(min = 8, max = 255)
        private String password;
    }

    @Getter
    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 8, max = 255)
        private String password;
    }

    public record TokenResponse(String accessToken) {
    }
}
