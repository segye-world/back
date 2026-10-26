package com.segye.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segye.auth.kakao.KakaoApiException;
import com.segye.auth.kakao.KakaoClient.KakaoUser;
import com.segye.auth.kakao.KakaoClient.TokenInfo;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.support.KakaoMockTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
@DisplayName("카카오 로그인 / JWT 인증")
class AuthApiTest extends KakaoMockTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    MemberRepository memberRepository;

    private long randomKakaoId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }

    private void givenKakaoUser(String kakaoToken, long kakaoId, String nickname) {
        given(kakaoClient.getTokenInfo(kakaoToken)).willReturn(new TokenInfo(kakaoId, OUR_APP_ID));
        given(kakaoClient.getUser(kakaoToken)).willReturn(new KakaoUser(kakaoId, nickname));
    }

    private ResultActions kakaoLogin(String kakaoToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("accessToken", kakaoToken))));
    }

    private Long memberIdFrom(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return tokenProvider.validateAndGetMemberId(om.readTree(body).path("data").path("accessToken").asText());
    }

    // --- 카카오 로그인 ---

    @Test
    @DisplayName("처음 로그인하면 회원을 만들고, 그 회원의 JWT 와 isNewMember=true 를 준다")
    void firstLoginCreatesMember() throws Exception {
        long kakaoId = randomKakaoId();
        givenKakaoUser("token-1", kakaoId, "세계");

        ResultActions result = kakaoLogin("token-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.isNewMember").value(true));

        Member member = memberRepository.findByProviderAndProviderId(AuthProvider.KAKAO, String.valueOf(kakaoId))
                .orElseThrow();
        assertThat(member.getNickname()).isEqualTo("세계");
        assertThat(memberIdFrom(result)).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("다시 로그인하면 같은 회원으로 로그인되고, 바뀐 닉네임을 반영한다")
    void secondLoginReusesMember() throws Exception {
        long kakaoId = randomKakaoId();
        givenKakaoUser("token-1", kakaoId, "세계");
        Long firstId = memberIdFrom(kakaoLogin("token-1").andExpect(status().isOk()));

        givenKakaoUser("token-2", kakaoId, "새 닉네임");
        ResultActions second = kakaoLogin("token-2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewMember").value(false));

        assertThat(memberIdFrom(second)).isEqualTo(firstId);
        assertThat(memberRepository.findById(firstId).orElseThrow().getNickname()).isEqualTo("새 닉네임");
    }

    @Test
    @DisplayName("다른 앱에서 발급된 카카오 토큰은 401 이고 회원을 만들지 않는다")
    void rejectsTokenIssuedForAnotherApp() throws Exception {
        long kakaoId = randomKakaoId();
        given(kakaoClient.getTokenInfo("foreign")).willReturn(new TokenInfo(kakaoId, OUR_APP_ID + 1));
        given(kakaoClient.getUser("foreign")).willReturn(new KakaoUser(kakaoId, "남의 앱 사용자"));

        kakaoLogin("foreign")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        assertThat(memberRepository.findByProviderAndProviderId(AuthProvider.KAKAO, String.valueOf(kakaoId)))
                .isEmpty();
    }

    @Test
    @DisplayName("토큰 정보와 사용자 정보의 회원번호가 다르면 401 이다")
    void rejectsMismatchedKakaoIds() throws Exception {
        given(kakaoClient.getTokenInfo("t")).willReturn(new TokenInfo(1L, OUR_APP_ID));
        given(kakaoClient.getUser("t")).willReturn(new KakaoUser(2L, "x"));

        kakaoLogin("t").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("카카오가 토큰을 거절하면 401 이다")
    void invalidKakaoTokenYields401() throws Exception {
        given(kakaoClient.getTokenInfo("expired"))
                .willThrow(new SecurityException("카카오 인증이 만료되었거나 올바르지 않습니다."));

        kakaoLogin("expired")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("카카오 서버 장애는 401 이 아니라 502 로 구분한다")
    void kakaoOutageYields502() throws Exception {
        given(kakaoClient.getTokenInfo("t")).willThrow(new KakaoApiException("timeout"));

        kakaoLogin("t")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("KAKAO_UNAVAILABLE"));
    }

    @Test
    @DisplayName("카카오 토큰이 비어 있으면 400 VALIDATION_ERROR 이다")
    void blankTokenYields400() throws Exception {
        kakaoLogin(" ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/v1/auth/login", "/api/v1/auth/signup", "/api/v1/auth/dev-login"})
    @DisplayName("이메일 로그인·회원가입과 기본 설정의 dev-login 경로는 없다")
    void removedOrDisabledEndpoints(String path) throws Exception {
        int status = mvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.c\",\"password\":\"password123\",\"memberId\":1}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isBetween(400, 499);
    }

    // --- JWT 인증 ---

    @Test
    @DisplayName("인증 없이 보호된 API 를 호출하면 401 이다")
    void protectedEndpointRequiresAuth() throws Exception {
        mvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // JwtAuthFilter 는 서블릿 필터라 GlobalExceptionHandler(@RestControllerAdvice) 의 사정권 밖이다.
    // 필터가 JwtTokenProvider 의 SecurityException 을 그대로 던지면 예외가 필터 체인 밖으로 빠져나가
    // 401 이 나가지 않는다.

    private String signedWith(String secret, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .setSubject("1")
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private void expectUnauthorizedWith(String token) throws Exception {
        mvc.perform(get("/api/v1/schedules")
                        .param("date", "2026-09-08")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"not-a-jwt", "aaa.bbb.ccc", " "})
    @DisplayName("형식이 깨진 토큰은 500 이 아니라 401 이다")
    void malformedTokenYields401(String token) throws Exception {
        expectUnauthorizedWith(token);
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 401 이다")
    void wrongSignatureYields401() throws Exception {
        String otherSecret = "ANOTHER_SECRET_LONG_ENOUGH_FOR_HS256_ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Instant now = Instant.now();

        expectUnauthorizedWith(signedWith(otherSecret, now, now.plusSeconds(600)));
    }

    @Test
    @DisplayName("만료된 토큰은 401 이다")
    void expiredTokenYields401() throws Exception {
        Instant now = Instant.now();

        expectUnauthorizedWith(
                signedWith(jwtProperties.getSecret(), now.minusSeconds(7200), now.minusSeconds(3600)));
    }

    @Test
    @DisplayName("무효한 토큰이 붙어 있어도 permitAll 경로는 정상 동작한다")
    void invalidTokenDoesNotBreakPublicEndpoint() throws Exception {
        mvc.perform(get("/health").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효한 토큰이면 보호된 API 에 접근할 수 있다")
    void validTokenGrantsAccess() throws Exception {
        Long memberId = memberRepository.save(
                new Member(AuthProvider.DEV, "jwt-" + UUID.randomUUID(), "tester")).getId();

        mvc.perform(get("/api/v1/schedules")
                        .param("date", "2026-09-08")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenProvider.createAccessToken(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
