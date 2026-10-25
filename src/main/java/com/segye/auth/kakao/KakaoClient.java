package com.segye.auth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

/**
 * 카카오 REST API 호출만 담당한다. 토큰이 우리 앱에서 발급됐는지 같은 판단은 AuthService 가 한다.
 *
 * - 사용자 토큰이 잘못됐거나 만료됨(4xx): SecurityException → 401
 * - 카카오 장애·타임아웃(5xx, I/O): KakaoApiException → 502
 */
@Component
public class KakaoClient {

    private final RestClient restClient;
    private final KakaoProperties props;

    public KakaoClient(@Qualifier("kakaoRestClient") RestClient restClient, KakaoProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    /** 토큰의 회원번호와 발급 앱 ID. 다른 앱에서 발급된 토큰을 걸러내는 데 쓴다. */
    public TokenInfo getTokenInfo(String accessToken) {
        return call(() -> restClient.get()
                .uri("/v1/user/access_token_info")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new SecurityException("카카오 인증이 만료되었거나 올바르지 않습니다.");
                })
                .body(TokenInfo.class));
    }

    public KakaoUser getUser(String accessToken) {
        UserResponse res = call(() -> restClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, r) -> {
                    throw new SecurityException("카카오 인증이 만료되었거나 올바르지 않습니다.");
                })
                .body(UserResponse.class));

        String nickname = null;
        if (res.kakaoAccount() != null && res.kakaoAccount().profile() != null) {
            nickname = res.kakaoAccount().profile().nickname();
        }
        return new KakaoUser(res.id(), nickname);
    }

    /** 어드민 키로 우리 앱과 사용자의 연결을 끊는다. 실패하면 예외를 던지므로 호출부에서 처리한다. */
    public void unlink(long kakaoUserId) {
        if (props.getAdminKey() == null || props.getAdminKey().isBlank()) {
            throw new KakaoApiException("카카오 어드민 키가 설정되지 않았습니다.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", String.valueOf(kakaoUserId));

        call(() -> restClient.post()
                .uri("/v1/user/unlink")
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + props.getAdminKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new KakaoApiException("카카오 연결 끊기에 실패했습니다. status=" + res.getStatusCode());
                })
                .toBodilessEntity());
    }

    private <T> T call(Supplier<T> request) {
        try {
            T body = request.get();
            if (body == null) {
                throw new KakaoApiException("카카오 응답이 비어 있습니다.");
            }
            return body;
        } catch (RestClientException e) {
            // 위 onStatus 에서 처리하지 않은 5xx 와 연결 실패·타임아웃이 여기로 온다.
            throw new KakaoApiException("카카오 서버와 통신하지 못했습니다.", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenInfo(Long id, @JsonProperty("app_id") Long appId) {
    }

    public record KakaoUser(Long id, String nickname) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(Profile profile) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(String nickname) {
    }
}
