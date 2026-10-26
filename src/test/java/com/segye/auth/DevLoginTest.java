package com.segye.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 꺼져 있는 기본 설정에서 경로가 없는 것은 AuthApiTest 에서 확인한다.
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.auth.dev-login-enabled=true")
@DisplayName("개발용 로그인(dev-login)")
class DevLoginTest extends PostgresTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("켜져 있으면 memberId 로 그 회원의 JWT 를 받는다")
    void issuesTokenForMember() throws Exception {
        Long memberId = memberRepository.save(
                new Member(AuthProvider.DEV, "dev-" + UUID.randomUUID(), "개발")).getId();

        String body = mvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("memberId", memberId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewMember").value(false))
                .andReturn().getResponse().getContentAsString();

        String token = om.readTree(body).path("data").path("accessToken").asText();
        assertThat(tokenProvider.validateAndGetMemberId(token)).isEqualTo(memberId);
    }

    @Test
    @DisplayName("없는 회원이면 400 이다")
    void rejectsUnknownMember() throws Exception {
        mvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("memberId", Long.MAX_VALUE))))
                .andExpect(status().isBadRequest());
    }
}
