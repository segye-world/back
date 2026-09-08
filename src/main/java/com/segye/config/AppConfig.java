package com.segye.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// JwtProperties는 SegyeApplication의 @EnableConfigurationProperties에서 등록한다.
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원가입/로그인에서 공통으로 사용할 비밀번호 해시 인코더 빈을 등록한다.
        return new BCryptPasswordEncoder();
    }
}
