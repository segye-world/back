package com.segye;

import com.segye.auth.JwtProperties;
import com.segye.auth.kakao.KakaoProperties;
import com.segye.auth.review.ReviewLoginProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({JwtProperties.class, KakaoProperties.class, ReviewLoginProperties.class})
@SpringBootApplication
public class SegyeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SegyeApplication.class, args);
    }
}