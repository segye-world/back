package com.segye.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void storesHashedPassword(@Autowired com.segye.member.MemberRepository memberRepository) throws Exception {
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
}
