package com.segye.auth.kakao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class KakaoConfig {

    @Bean
    public RestClient kakaoRestClient(RestClient.Builder builder, KakaoProperties props) {
        // 카카오가 느려도 로그인 요청 스레드가 무한정 묶이지 않도록 타임아웃을 건다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.getReadTimeout());

        return builder
                .baseUrl(props.getApiBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
