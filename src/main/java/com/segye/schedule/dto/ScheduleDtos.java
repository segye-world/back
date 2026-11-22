package com.segye.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 일정 시간은 하루 시작(00:00)부터의 분으로 주고받는다. 예: 14:30 = 870, 24:00 = 1440.
 *
 * 앱이 분 단위로 바뀌기 전까지는 기존 startHour/endHour 도 받는다.
 * 요청에 startMinute/endMinute 가 있으면 그 값을 쓰고, 없으면 startHour/endHour 를 분으로 바꿔 쓴다.
 */
public class ScheduleDtos {

    public record CreateRequest(
            @NotBlank String title,
            @NotNull LocalDate date,
            @Min(0) @Max(1439) Integer startMinute,
            @Min(1) @Max(1440) Integer endMinute,
            @Deprecated @Min(0) @Max(23) Integer startHour,
            @Deprecated @Min(1) @Max(24) Integer endHour,
            @NotBlank String colorHex
    ) {}

    public record UpdateRequest(
            @NotBlank String title,
            @Min(0) @Max(1439) Integer startMinute,
            @Min(1) @Max(1440) Integer endMinute,
            @Deprecated @Min(0) @Max(23) Integer startHour,
            @Deprecated @Min(1) @Max(24) Integer endHour,
            @NotBlank String colorHex
    ) {}

    /**
     * startHour/endHour 는 아직 분 단위를 모르는 앱 버전을 위한 값이다.
     * 시작은 내림, 종료는 올림해서 일정이 차지하는 시간 칸을 모두 덮는다. (14:30~16:10 → 14~17)
     */
    public record ScheduleResponse(
            Long id,
            String title,
            LocalDate date,
            Integer startMinute,
            Integer endMinute,
            @Deprecated Integer startHour,
            @Deprecated Integer endHour,
            String colorHex
    ) {}
}
