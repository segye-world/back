package com.segye.account;

import com.segye.account.dto.AccountRecordDtos;
import com.segye.budget.BudgetService;
import com.segye.budget.dto.BudgetDtos;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.category.CategoryType;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("가계부 요약/카테고리별 집계 서비스")
class AccountRecordSummaryServiceTest extends PostgresTestSupport {

    @Autowired
    AccountRecordService accountRecordService;

    @Autowired
    BudgetService budgetService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    private Long memberId;
    private Long foodCategoryId;
    private Long transportCategoryId;
    private Long salaryCategoryId;

    @BeforeEach
    void setUp() {
        memberId = memberRepository.save(new Member("summary-" + UUID.randomUUID() + "@example.com", "hash")).getId();
        foodCategoryId = categoryRepository.save(new Category("식비", CategoryType.EXPENSE)).getId();
        transportCategoryId = categoryRepository.save(new Category("교통", CategoryType.EXPENSE)).getId();
        salaryCategoryId = categoryRepository.save(new Category("급여", CategoryType.INCOME)).getId();
    }

    private void record(Long categoryId, long amount, LocalDateTime time) {
        accountRecordService.create(memberId,
                new AccountRecordDtos.CreateRequest(categoryId, amount, time, null, null));
    }

    @Test
    @DisplayName("이번 달 수입/지출 합계와 카테고리별 지출을 집계한다")
    void aggregatesIncomeAndExpense() {
        record(salaryCategoryId, 3000000L, LocalDateTime.of(2026, 9, 1, 9, 0));
        record(foodCategoryId, -50000L, LocalDateTime.of(2026, 9, 5, 12, 0));
        record(foodCategoryId, 30000L, LocalDateTime.of(2026, 9, 10, 18, 0));
        record(transportCategoryId, 20000L, LocalDateTime.of(2026, 9, 15, 8, 0));
        // 다른 달 기록은 집계에서 제외되어야 한다
        record(foodCategoryId, 99999L, LocalDateTime.of(2026, 8, 1, 12, 0));

        AccountRecordDtos.SummaryResponse summary = accountRecordService.summary(memberId, 2026, 9);

        assertThat(summary.totalIncome()).isEqualTo(3000000L);
        assertThat(summary.totalExpense()).isEqualTo(100000L);
        assertThat(summary.categoryBreakdown()).hasSize(2);
        assertThat(summary.categoryBreakdown().get(0).categoryName()).isEqualTo("식비");
        assertThat(summary.categoryBreakdown().get(0).amount()).isEqualTo(80000L);
        assertThat(summary.categoryBreakdown().get(1).categoryName()).isEqualTo("교통");
        assertThat(summary.categoryBreakdown().get(1).amount()).isEqualTo(20000L);
    }

    @Test
    @DisplayName("한도가 없으면 잔여 예산은 null이다")
    void remainingBudgetIsNullWithoutLimit() {
        record(foodCategoryId, 50000L, LocalDateTime.of(2026, 9, 5, 12, 0));

        AccountRecordDtos.SummaryResponse summary = accountRecordService.summary(memberId, 2026, 9);

        assertThat(summary.remainingBudget()).isNull();
    }

    @Test
    @DisplayName("한도가 있으면 잔여 예산 = 한도 - 지출")
    void remainingBudgetIsLimitMinusExpense() {
        budgetService.upsert(memberId, new BudgetDtos.UpsertRequest(2026, 9, 200000L));
        record(foodCategoryId, 50000L, LocalDateTime.of(2026, 9, 5, 12, 0));

        AccountRecordDtos.SummaryResponse summary = accountRecordService.summary(memberId, 2026, 9);

        assertThat(summary.remainingBudget()).isEqualTo(150000L);
    }

    @Test
    @DisplayName("수입이 0이면 저축률은 null이다")
    void savingRateIsNullWhenNoIncome() {
        record(foodCategoryId, 50000L, LocalDateTime.of(2026, 9, 5, 12, 0));

        AccountRecordDtos.SummaryResponse summary = accountRecordService.summary(memberId, 2026, 9);

        assertThat(summary.savingRate()).isNull();
    }

    @Test
    @DisplayName("저축률 = (수입-지출)/수입 * 100")
    void computesSavingRate() {
        record(salaryCategoryId, 1000000L, LocalDateTime.of(2026, 9, 1, 9, 0));
        record(foodCategoryId, 300000L, LocalDateTime.of(2026, 9, 5, 12, 0));

        AccountRecordDtos.SummaryResponse summary = accountRecordService.summary(memberId, 2026, 9);

        assertThat(summary.savingRate()).isEqualTo(70.0);
    }

    @Test
    @DisplayName("최근 거래 내역은 최신순으로 정렬된다")
    void recentTransactionsAreSortedByLatest() {
        record(foodCategoryId, 10000L, LocalDateTime.of(2026, 9, 1, 9, 0));
        record(foodCategoryId, 20000L, LocalDateTime.of(2026, 9, 10, 9, 0));
        record(foodCategoryId, 30000L, LocalDateTime.of(2026, 9, 20, 9, 0));

        AccountRecordDtos.SummaryResponse summary = accountRecordService.summary(memberId, 2026, 9);

        assertThat(summary.recentTransactions())
                .extracting(AccountRecordDtos.AccountRecordResponse::amount)
                .containsExactly(30000L, 20000L, 10000L);
    }

    @Test
    @DisplayName("카테고리별 집계는 지정한 달로 필터링하고 비율을 계산한다")
    void categorySummaryForMonth() {
        record(foodCategoryId, 30000L, LocalDateTime.of(2026, 9, 5, 12, 0));
        record(transportCategoryId, 70000L, LocalDateTime.of(2026, 9, 6, 12, 0));
        record(foodCategoryId, 99999L, LocalDateTime.of(2026, 8, 1, 12, 0));

        AccountRecordDtos.CategorySummaryResponse result =
                accountRecordService.categorySummary(memberId, 2026, 9, CategoryType.EXPENSE);

        assertThat(result.totalAmount()).isEqualTo(100000L);
        assertThat(result.categories()).hasSize(2);
        assertThat(result.categories().get(0).categoryName()).isEqualTo("교통");
        assertThat(result.categories().get(0).ratio()).isEqualTo(70.0);
        assertThat(result.categories().get(1).categoryName()).isEqualTo("식비");
        assertThat(result.categories().get(1).ratio()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("year/month를 생략하면 전체 기간을 집계한다")
    void categorySummaryForAllTimeWhenMonthOmitted() {
        record(foodCategoryId, 30000L, LocalDateTime.of(2026, 9, 5, 12, 0));
        record(foodCategoryId, 20000L, LocalDateTime.of(2025, 1, 1, 12, 0));

        AccountRecordDtos.CategorySummaryResponse result =
                accountRecordService.categorySummary(memberId, null, null, CategoryType.EXPENSE);

        assertThat(result.totalAmount()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("year와 month 중 하나만 전달하면 예외가 발생한다")
    void rejectsPartialYearMonth() {
        assertThatThrownBy(() -> accountRecordService.categorySummary(memberId, 2026, null, CategoryType.EXPENSE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
