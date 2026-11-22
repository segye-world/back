package com.segye.auth.review;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 스토어 심사용 로그인 설정. 아이디와 비밀번호 해시는 환경변수로만 넣고 저장소에는 두지 않는다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth.review-login")
public class ReviewLoginProperties {
    private boolean enabled = false;
    private String loginId;
    // 평문이 아닌 bcrypt 해시($2a$/$2b$/$2y$...)다.
    private String passwordHash;
    // 연속으로 이만큼 틀리면 lockDuration 동안 맞는 비밀번호도 받지 않는다.
    private int maxFailures = 10;
    private Duration lockDuration = Duration.ofMinutes(10);
}
