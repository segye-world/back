package com.segye.account.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class AccountRecordDtos {

    public record CreateRequest(
            @NotNull Long categoryId,
            @NotNull Long amount,
            @NotNull LocalDateTime transactionTime,
            Long scheduleId
    ) {
    }

    public record UpdateRequest(
            @NotNull Long categoryId,
            @NotNull Long amount,
            @NotNull LocalDateTime transactionTime,
            Long scheduleId
    ) {
    }

    public record AccountRecordResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String categoryType,
            Long amount,
            LocalDateTime transactionTime,
            Long scheduleId
    ) {
    }
}
