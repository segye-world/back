package com.segye.account.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class AccountRecordDtos {

    public record CreateRequest(
            @NotNull Long categoryId,
            @NotNull Long amount,
            @NotNull LocalDateTime transactionTime,
            Long scheduleId,
            Long sourceCategoryId  // 수입원(=지출 수단) 카테고리 id, nullable — 수입 기록처럼 필요 없는 경우
    ) {
    }

    public record UpdateRequest(
            @NotNull Long categoryId,
            @NotNull Long amount,
            @NotNull LocalDateTime transactionTime,
            Long scheduleId,
            Long sourceCategoryId  // null 이면 연결 해제
    ) {
    }

    public record AccountRecordResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String categoryType,
            Long sourceCategoryId,
            String sourceCategoryName,
            Long amount,
            LocalDateTime transactionTime,
            Long scheduleId
    ) {
    }

    public record CategoryAmount(
            Long categoryId,
            String categoryName,
            Long amount
    ) {
    }

    public record SummaryResponse(
            Long totalIncome,
            Long totalExpense,
            Long remainingBudget,   // 한도 미설정 시 null
            Double savingRate,      // 이번 달 수입이 0이면 null. (수입-지출)/수입*100
            List<CategoryAmount> categoryBreakdown,
            List<AccountRecordResponse> recentTransactions
    ) {
    }

    public record CategorySummaryItem(
            Long categoryId,
            String categoryName,
            Long amount,
            double ratio   // 전체 합계 대비 비율(%). 합계가 0이면 0
    ) {
    }

    public record CategorySummaryResponse(
            String type,
            Long totalAmount,
            List<CategorySummaryItem> categories
    ) {
    }
}
