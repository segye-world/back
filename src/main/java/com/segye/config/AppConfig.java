package com.segye.config;

import com.segye.auth.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원가입/로그인에서 공통으로 사용할 비밀번호 해시 인코더 빈을 등록한다.
        return new BCryptPasswordEncoder();
    }
}
