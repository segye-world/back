package com.segye.account.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class AccountRecordDtos {

    public record CreateRequest(
            @NotNull Long categoryId,
            @NotNull Long amount,
            @NotNull LocalDateTime transactionTime,
            Long scheduleId,
            Long paymentMethodId  // nullable — 수입 기록처럼 수단이 없는 경우
    ) {
    }

    public record UpdateRequest(
            @NotNull Long categoryId,
            @NotNull Long amount,
            @NotNull LocalDateTime transactionTime,
            Long scheduleId,
            Long paymentMethodId  // null 이면 연결 해제
    ) {
    }

    public record AccountRecordResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String categoryType,
            Long paymentMethodId,
            String paymentMethodName,
            Long amount,
            LocalDateTime transactionTime,
            Long scheduleId
    ) {
    }
}
