package com.segye.auth.kakao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("KakaoClient")
class KakaoClientTest {

    private static final String BASE = "https://kapi.kakao.com";

    private MockRestServiceServer server;
    private KakaoProperties props;
    private KakaoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        props = new KakaoProperties();
        props.setAdminKey("test-admin-key");
        client = new KakaoClient(builder.build(), props);
    }

    @Test
    @DisplayName("토큰 정보 조회는 Bearer 토큰을 보내고 회원번호·앱 ID 를 읽는다")
    void getTokenInfo() {
        server.expect(requestTo(BASE + "/v1/user/access_token_info"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-token"))
                .andRespond(withSuccess("""
                        {"id": 4321, "expires_in": 7199, "app_id": 1000001}
                        """, MediaType.APPLICATION_JSON));

        KakaoClient.TokenInfo info = client.getTokenInfo("kakao-token");

        assertThat(info.id()).isEqualTo(4321L);
        assertThat(info.appId()).isEqualTo(1000001L);
        server.verify();
    }

    @Test
    @DisplayName("사용자 정보에서 회원번호와 닉네임을 읽는다")
    void getUser() {
        server.expect(requestTo(BASE + "/v2/user/me"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-token"))
                .andRespond(withSuccess("""
                        {"id": 4321, "connected_at": "2026-10-25T00:00:00Z",
                         "kakao_account": {"profile_nickname_needs_agreement": false,
                                           "profile": {"nickname": "세계", "is_default_nickname": false}}}
                        """, MediaType.APPLICATION_JSON));

        KakaoClient.KakaoUser user = client.getUser("kakao-token");

        assertThat(user.id()).isEqualTo(4321L);
        assertThat(user.nickname()).isEqualTo("세계");
    }

    @Test
    @DisplayName("닉네임 동의가 없어 profile 이 없으면 닉네임은 null 이다")
    void getUserWithoutProfile() {
        server.expect(requestTo(BASE + "/v2/user/me"))
                .andRespond(withSuccess("""
                        {"id": 4321, "kakao_account": {}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getUser("kakao-token").nickname()).isNull();
    }

    @Test
    @DisplayName("카카오가 토큰을 거절(401)하면 SecurityException 이다")
    void invalidTokenIsSecurityException() {
        server.expect(requestTo(BASE + "/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"msg": "this access token does not exist", "code": -401}
                                """));

        assertThatThrownBy(() -> client.getTokenInfo("expired")).isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("카카오 서버 오류(5xx)는 KakaoApiException 이다")
    void serverErrorIsKakaoApiException() {
        server.expect(requestTo(BASE + "/v1/user/access_token_info")).andRespond(withServerError());

        assertThatThrownBy(() -> client.getTokenInfo("t")).isInstanceOf(KakaoApiException.class);
    }

    @Test
    @DisplayName("연결 끊기는 어드민 키로 회원번호를 form 으로 보낸다")
    void unlink() {
        server.expect(requestTo(BASE + "/v1/user/unlink"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-admin-key"))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "target_id_type", "user_id",
                        "target_id", "4321")))
                .andRespond(withSuccess("""
                        {"id": 4321}
                        """, MediaType.APPLICATION_JSON));

        client.unlink(4321L);

        server.verify();
    }

    @Test
    @DisplayName("연결 끊기가 실패하면 KakaoApiException 이다")
    void unlinkFailure() {
        server.expect(requestTo(BASE + "/v1/user/unlink"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.unlink(4321L)).isInstanceOf(KakaoApiException.class);
    }

    @Test
    @DisplayName("어드민 키가 없으면 요청하지 않고 KakaoApiException 이다")
    void unlinkWithoutAdminKey() {
        props.setAdminKey("");

        assertThatThrownBy(() -> client.unlink(4321L)).isInstanceOf(KakaoApiException.class);
        server.verify();
    }
}
