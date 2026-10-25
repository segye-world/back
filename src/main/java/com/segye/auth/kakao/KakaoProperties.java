package com.segye.auth.kakao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kakao")
public class KakaoProperties {
    private String apiBaseUrl = "https://kapi.kakao.com";
    // 앱 관리 페이지 상단의 앱 ID. 비어 있으면 카카오 로그인은 모두 거절된다.
    private Long appId;
    // 탈퇴 시 연결 끊기에만 쓴다. 비어 있으면 연결 끊기를 건너뛴다.
    private String adminKey;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
}
