package com.segye.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segye.member.MemberRepository;
import com.segye.support.PostgresTestSupport;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
@DisplayName("회원가입/로그인 API")
class AuthApiTest extends PostgresTestSupport {

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

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private void signUp(String email, String password) throws Exception {
        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("가입 후 로그인하면 memberId 를 subject 로 가지는 JWT 가 발급된다")
    void signUpThenLogin() throws Exception {
        String email = uniqueEmail();
        signUp(email, "password123");

        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = om.readTree(body).path("data").path("accessToken").asText();
        assertThat(tokenProvider.validateAndGetMemberId(token)).isPositive();
    }

    @Test
    @DisplayName("비밀번호는 평문이 아니라 BCrypt 해시로 저장된다")
    void storesHashedPassword() throws Exception {
        String email = uniqueEmail();
        signUp(email, "password123");

        String stored = memberRepository.findByEmail(email).orElseThrow().getPassword();
        assertThat(stored).isNotEqualTo("password123").startsWith("$2");
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 400 BAD_REQUEST 로 거절한다")
    void rejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        signUp(email, "password123");

        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400 VALIDATION_ERROR 로 거절한다")
    void rejectsShortPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", uniqueEmail(), "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 을 반환하고, 존재하지 않는 계정과 같은 메시지를 준다")
    void rejectsWrongPasswordWithoutLeakingAccountExistence() throws Exception {
        String email = uniqueEmail();
        signUp(email, "password123");

        String wrongPw = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andReturn().getResponse().getContentAsString();

        String noSuchUser = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", uniqueEmail(), "password", "password123"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPw).isEqualTo(noSuchUser);
    }

    @Test
    @DisplayName("인증 없이 보호된 API 를 호출하면 401 이다")
    void protectedEndpointRequiresAuth() throws Exception {
        mvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- 무효 토큰 처리 ---
    //
    // JwtAuthFilter 는 서블릿 필터라 GlobalExceptionHandler(@RestControllerAdvice) 의 사정권 밖이다.
    // 필터가 JwtTokenProvider 의 SecurityException 을 그대로 던지면 예외가 필터 체인 밖으로 빠져나가
    // 401 이 나가지 않는다. (아래 테스트를 수정 전 코드로 돌리면 SecurityException 이 그대로 전파된다.
    // 서블릿 컨테이너에서는 500 에러 페이지가 된다.)

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
        String email = uniqueEmail();
        signUp(email, "password123");
        Long memberId = memberRepository.findByEmail(email).orElseThrow().getId();

        mvc.perform(get("/api/v1/schedules")
                        .param("date", "2026-09-08")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenProvider.createAccessToken(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
