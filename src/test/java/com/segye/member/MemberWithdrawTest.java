package com.segye.member;

import com.segye.auth.JwtTokenProvider;
import com.segye.auth.kakao.KakaoApiException;
import com.segye.support.KakaoMockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
@DisplayName("회원 탈퇴")
class MemberWithdrawTest extends KakaoMockTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Autowired
    MemberRepository memberRepository;

    private ResultActions withdraw(Member member) throws Exception {
        return mvc.perform(delete("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.createAccessToken(member.getId())));
    }

    private Member kakaoMember(long kakaoId) {
        return memberRepository.save(new Member(AuthProvider.KAKAO, String.valueOf(kakaoId), "탈퇴자"));
    }

    @Test
    @DisplayName("카카오 회원이 탈퇴하면 회원을 지우고 카카오 연결을 끊는다")
    void unlinksKakaoOnWithdraw() throws Exception {
        long kakaoId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        Member member = kakaoMember(kakaoId);

        withdraw(member).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        assertThat(memberRepository.findById(member.getId())).isEmpty();
        verify(kakaoClient).unlink(kakaoId);
    }

    @Test
    @DisplayName("연결 끊기가 실패해도 탈퇴는 성공한다")
    void withdrawSucceedsEvenIfUnlinkFails() throws Exception {
        long kakaoId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        Member member = kakaoMember(kakaoId);
        willThrow(new KakaoApiException("카카오 장애")).given(kakaoClient).unlink(kakaoId);

        withdraw(member).andExpect(status().isOk());

        assertThat(memberRepository.findById(member.getId())).isEmpty();
    }

    @Test
    @DisplayName("카카오 계정이 아닌 회원은 연결 끊기를 호출하지 않는다")
    void devMemberIsNotUnlinked() throws Exception {
        Member member = memberRepository.save(new Member(AuthProvider.DEV, "dev-" + UUID.randomUUID(), "개발"));

        withdraw(member).andExpect(status().isOk());

        assertThat(memberRepository.findById(member.getId())).isEmpty();
        verify(kakaoClient, never()).unlink(anyLong());
    }
}
