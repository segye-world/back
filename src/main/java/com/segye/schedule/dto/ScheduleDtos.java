package com.segye.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ScheduleDtos {

    public record CreateRequest(
            @NotBlank String title,
            @NotNull LocalDate date,
            @NotNull @Min(0) @Max(23) Integer startHour,
            @NotNull @Min(1) @Max(24) Integer endHour,
            @NotBlank String colorHex
    ) {}

    public record UpdateRequest(
            @NotBlank String title,
            @NotNull @Min(0) @Max(23) Integer startHour,
            @NotNull @Min(1) @Max(24) Integer endHour,
            @NotBlank String colorHex
    ) {}

    public record ScheduleResponse(
            Long id,
            String title,
            LocalDate date,
            Integer startHour,
            Integer endHour,
            String colorHex
    ) {}
}
