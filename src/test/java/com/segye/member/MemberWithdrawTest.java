package com.segye.member;

import com.segye.auth.JwtTokenProvider;
import com.segye.auth.kakao.KakaoApiException;
import com.segye.budget.Budget;
import com.segye.budget.BudgetRepository;
import com.segye.account.AccountRecord;
import com.segye.account.AccountRecordRepository;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.category.CategoryType;
import com.segye.support.KakaoMockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Autowired
    BudgetRepository budgetRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AccountRecordRepository accountRecordRepository;

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
    @DisplayName("회원 탈퇴 시 해당 회원의 월별 예산만 삭제한다")
    void deletesOnlyWithdrawingMembersBudgets() throws Exception {
        long kakaoId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        Member member = kakaoMember(kakaoId);
        Member otherMember = memberRepository.save(new Member(AuthProvider.DEV, "dev-" + UUID.randomUUID(), "다른 회원"));
        Budget firstBudget = budgetRepository.save(new Budget(member, 2026, 10, 100_000L));
        Budget secondBudget = budgetRepository.save(new Budget(member, 2026, 11, 120_000L));
        Budget otherBudget = budgetRepository.save(new Budget(otherMember, 2026, 10, 150_000L));

        withdraw(member).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        // 테스트 트랜잭션이 롤백되기 전에 실제 DELETE와 외래 키 검사를 실행한다.
        memberRepository.flush();
        assertThat(memberRepository.existsById(member.getId())).isFalse();
        assertThat(budgetRepository.existsById(firstBudget.getId())).isFalse();
        assertThat(budgetRepository.existsById(secondBudget.getId())).isFalse();
        assertThat(budgetRepository.existsById(otherBudget.getId())).isTrue();
        verify(kakaoClient).unlink(kakaoId);
    }

    @Test
    @DisplayName("내 거래와 카테고리는 삭제하고 다른 회원의 카테고리는 보존한다")
    void deletesOnlyWithdrawingMembersCategories() throws Exception {
        Member member = kakaoMember(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        Member other = memberRepository.save(new Member(AuthProvider.DEV, "dev-" + UUID.randomUUID(), "다른 회원"));
        Category expense = categoryRepository.save(new Category(member, "식비", CategoryType.EXPENSE));
        Category source = categoryRepository.save(new Category(member, "현금", CategoryType.INCOME));
        Category otherCategory = categoryRepository.save(new Category(other, "식비", CategoryType.EXPENSE));
        AccountRecord record = accountRecordRepository.save(
                new AccountRecord(member, expense, source, -1000L, LocalDateTime.now(), null, null));

        withdraw(member).andExpect(status().isOk());
        memberRepository.flush();

        assertThat(accountRecordRepository.existsById(record.getId())).isFalse();
        assertThat(categoryRepository.findByMember_IdOrderById(member.getId())).isEmpty();
        assertThat(categoryRepository.existsById(otherCategory.getId())).isTrue();
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
