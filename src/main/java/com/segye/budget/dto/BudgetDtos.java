package com.segye.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BudgetDtos {

    public record UpsertRequest(
            @NotNull Integer year,
            @NotNull Integer month,
            @NotNull @Positive Long limitAmount
    ) {
    }

    public record BudgetResponse(
            Long id,
            Integer year,
            Integer month,
            Long limitAmount
    ) {
    }
}
