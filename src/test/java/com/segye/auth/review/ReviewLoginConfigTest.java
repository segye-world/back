package com.segye.auth.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("심사용 로그인 설정 검증")
class ReviewLoginConfigTest {

    private ReviewLoginService serviceWith(String loginId, String passwordHash) {
        ReviewLoginProperties props = new ReviewLoginProperties();
        props.setEnabled(true);
        props.setLoginId(loginId);
        props.setPasswordHash(passwordHash);
        return new ReviewLoginService(props, null, null, null, null);
    }

    @Test
    @DisplayName("아이디가 비어 있으면 부팅을 막는다")
    void requiresLoginId() {
        String hash = new BCryptPasswordEncoder(4).encode("pw");
        assertThatThrownBy(() -> serviceWith("", hash).validateConfig()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> serviceWith(null, hash).validateConfig()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("비밀번호 해시가 없거나 평문이면 부팅을 막는다")
    void requiresBcryptHash() {
        assertThatThrownBy(() -> serviceWith("id", null).validateConfig()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> serviceWith("id", "plain-password").validateConfig())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("htpasswd 가 만드는 $2y$ 해시도 받는다")
    void acceptsBcryptVariants() {
        String hash2a = new BCryptPasswordEncoder(4).encode("pw");
        String hash2y = "$2y$" + hash2a.substring(4);
        assertThatCode(() -> serviceWith("id", hash2a).validateConfig()).doesNotThrowAnyException();
        assertThatCode(() -> serviceWith("id", hash2y).validateConfig()).doesNotThrowAnyException();
        // $2y$ 로 적어도 같은 비밀번호로 검증된다.
        org.assertj.core.api.Assertions.assertThat(new BCryptPasswordEncoder().matches("pw", hash2y)).isTrue();
    }
}
