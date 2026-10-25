package com.segye.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthDtos {

    // 앱이 카카오 SDK 로 로그인해서 받은 카카오 액세스 토큰. 서버는 검증에만 쓰고 저장하지 않는다.
    public record KakaoLoginRequest(@NotBlank String accessToken) {
    }

    public record DevLoginRequest(@NotNull Long memberId) {
    }

    // isNewMember 는 첫 가입 여부다. 앱은 이 값으로 약관 동의·온보딩을 띄울지 정한다.
    public record TokenResponse(String accessToken,
                                @JsonProperty("isNewMember") boolean isNewMember) {
    }
}
