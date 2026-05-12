package com.segye.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class TodoDtos {

    public record CreateRequest(
            @NotBlank String label,
            @NotNull LocalDate date,
            Long scheduleId  // nullable — null이면 일정 외 할일
    ) {}

    public record UpdateRequest(
            String label,    // null이면 수정 안 함
            Boolean isDone   // null이면 수정 안 함
    ) {}

    public record TodoResponse(
            Long id,
            Long scheduleId,
            String label,
            Boolean isDone,
            LocalDate date
    ) {}
}
