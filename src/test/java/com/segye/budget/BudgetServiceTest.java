package com.segye.budget;

import com.segye.budget.dto.BudgetDtos;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("월별 지출 한도 서비스")
class BudgetServiceTest extends PostgresTestSupport {

    @Autowired
    BudgetService budgetService;

    @Autowired
    MemberRepository memberRepository;

    private Long memberId;
    private Long otherMemberId;

    @BeforeEach
    void setUp() {
        memberId = newMember().getId();
        otherMemberId = newMember().getId();
    }

    private Member newMember() {
        return memberRepository.save(new Member("budget-" + UUID.randomUUID() + "@example.com", "hash"));
    }

    private BudgetDtos.UpsertRequest request(int year, int month, long limitAmount) {
        return new BudgetDtos.UpsertRequest(year, month, limitAmount);
    }

    @Test
    @DisplayName("한도가 없으면 null을 반환한다")
    void returnsNullWhenNotSet() {
        assertThat(budgetService.get(memberId, 2026, 9)).isNull();
    }

    @Test
    @DisplayName("한도를 설정하고 조회한다")
    void setsAndGetsBudget() {
        BudgetDtos.BudgetResponse created = budgetService.upsert(memberId, request(2026, 9, 500000L));

        assertThat(created.id()).isNotNull();
        assertThat(created.limitAmount()).isEqualTo(500000L);

        BudgetDtos.BudgetResponse found = budgetService.get(memberId, 2026, 9);
        assertThat(found.limitAmount()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("같은 달에 다시 설정하면 새로 만들지 않고 갱신한다")
    void upsertUpdatesExistingBudget() {
        Long firstId = budgetService.upsert(memberId, request(2026, 9, 500000L)).id();

        BudgetDtos.BudgetResponse updated = budgetService.upsert(memberId, request(2026, 9, 700000L));

        assertThat(updated.id()).isEqualTo(firstId);
        assertThat(updated.limitAmount()).isEqualTo(700000L);
        assertThat(budgetService.get(memberId, 2026, 9).limitAmount()).isEqualTo(700000L);
    }

    @Test
    @DisplayName("월이 다르면 별개의 한도로 관리한다")
    void keepsSeparateBudgetsPerMonth() {
        budgetService.upsert(memberId, request(2026, 8, 300000L));
        budgetService.upsert(memberId, request(2026, 9, 500000L));

        assertThat(budgetService.get(memberId, 2026, 8).limitAmount()).isEqualTo(300000L);
        assertThat(budgetService.get(memberId, 2026, 9).limitAmount()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("다른 회원의 한도는 조회되지 않는다")
    void doesNotLeakOtherMembersBudget() {
        budgetService.upsert(memberId, request(2026, 9, 500000L));

        assertThat(budgetService.get(otherMemberId, 2026, 9)).isNull();
    }
}
