package com.segye.auth.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segye.auth.JwtTokenProvider;
import com.segye.category.CategoryRepository;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.support.KakaoMockTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 꺼져 있는 기본 설정에서 경로가 없는 것은 AuthApiTest 에서 확인한다.
@AutoConfigureMockMvc
@Transactional
@DisplayName("심사용 로그인(review-login)")
class ReviewLoginTest extends KakaoMockTestSupport {

    static final String LOGIN_ID = "reviewer@onharu.test";
    static final String PASSWORD = "review-pass-1234";
    static final int MAX_FAILURES = 3;

    @DynamicPropertySource
    static void reviewLoginProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.review-login.enabled", () -> "true");
        registry.add("app.auth.review-login.login-id", () -> LOGIN_ID);
        registry.add("app.auth.review-login.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
        registry.add("app.auth.review-login.max-failures", () -> MAX_FAILURES);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ReviewLoginService reviewLoginService;

    @BeforeEach
    @AfterEach
    void resetLock() {
        reviewLoginService.resetFailures();
        reviewLoginService.clock = Clock.systemUTC();
    }

    private ResultActions login(String loginId, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/review-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("loginId", loginId, "password", password))));
    }

    private JsonNode loginOk() throws Exception {
        String body = login(LOGIN_ID, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data");
    }

    private Member reviewer() {
        return memberRepository.findByProviderAndProviderId(AuthProvider.REVIEW, ReviewLoginService.REVIEWER_PROVIDER_ID)
                .orElseThrow();
    }

    @Test
    @DisplayName("첫 로그인이면 심사용 회원을 기본 카테고리와 함께 만들고 JWT 를 준다")
    void firstLoginCreatesReviewer() throws Exception {
        JsonNode data = loginOk();

        assertThat(data.path("isNewMember").asBoolean()).isTrue();
        assertThat(data.path("refreshToken").asText()).isNotBlank();
        Member reviewer = reviewer();
        assertThat(reviewer.getNickname()).isEqualTo(ReviewLoginService.REVIEWER_NICKNAME);
        assertThat(tokenProvider.validateAndGetMemberId(data.path("accessToken").asText())).isEqualTo(reviewer.getId());
        assertThat(categoryRepository.findByMember_IdOrderById(reviewer.getId())).isNotEmpty();
    }

    @Test
    @DisplayName("다시 로그인하면 같은 심사용 회원으로 들어간다")
    void secondLoginReusesReviewer() throws Exception {
        loginOk();
        Long reviewerId = reviewer().getId();

        JsonNode data = loginOk();

        assertThat(data.path("isNewMember").asBoolean()).isFalse();
        assertThat(tokenProvider.validateAndGetMemberId(data.path("accessToken").asText())).isEqualTo(reviewerId);
    }

    @Test
    @DisplayName("아이디나 비밀번호가 틀리면 401 이고 회원을 만들지 않는다")
    void rejectsWrongCredentials() throws Exception {
        login(LOGIN_ID, "wrong-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        login("someone@else.test", PASSWORD).andExpect(status().isUnauthorized());

        assertThat(memberRepository.findByProviderAndProviderId(
                AuthProvider.REVIEW, ReviewLoginService.REVIEWER_PROVIDER_ID)).isEmpty();
    }

    @Test
    @DisplayName("빈 값이나 너무 긴 비밀번호는 400 이다")
    void rejectsInvalidRequest() throws Exception {
        login("", PASSWORD).andExpect(status().isBadRequest());
        login(LOGIN_ID, "x".repeat(65)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("연속으로 틀리면 잠기고, 잠금 시간이 지나면 다시 로그인할 수 있다")
    void locksAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < MAX_FAILURES; i++) {
            login(LOGIN_ID, "wrong-" + i).andExpect(status().isUnauthorized());
        }

        login(LOGIN_ID, PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));

        reviewLoginService.clock = Clock.offset(Clock.systemUTC(), Duration.ofMinutes(11));
        login(LOGIN_ID, PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("성공하면 실패 횟수가 초기화된다")
    void successResetsFailureCount() throws Exception {
        for (int i = 0; i < MAX_FAILURES - 1; i++) {
            login(LOGIN_ID, "wrong-" + i).andExpect(status().isUnauthorized());
        }
        loginOk();

        login(LOGIN_ID, "wrong-again").andExpect(status().isUnauthorized());
        loginOk();
    }

    @Test
    @DisplayName("심사자가 탈퇴하면 카카오 연결 끊기 없이 지워지고, 다음 로그인 때 새로 만들어진다")
    void withdrawThenLoginAgain() throws Exception {
        String accessToken = loginOk().path("accessToken").asText();
        Long firstId = reviewer().getId();

        mvc.perform(delete("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
        memberRepository.flush();

        assertThat(memberRepository.existsById(firstId)).isFalse();
        verify(kakaoClient, never()).unlink(anyLong());

        JsonNode data = loginOk();
        assertThat(data.path("isNewMember").asBoolean()).isTrue();
        assertThat(reviewer().getId()).isNotEqualTo(firstId);
    }
}
