package com.segye.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 조회용 날짜 구간. from·to 모두 포함한다.
 *
 * 일정·할 일 목록 조회는 {@code date} 하나 또는 {@code from}·{@code to} 구간으로 요청할 수 있다.
 * 한 번에 너무 많은 날을 읽지 않도록 구간은 최대 {@value #MAX_DAYS}일로 제한한다.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 31;

    /**
     * 요청 파라미터를 구간으로 바꾼다. date 가 있으면 그 하루, 없으면 from~to 를 쓴다.
     * 값이 빠졌거나 순서가 뒤집혔거나 너무 길면 {@link IllegalArgumentException} (400) 을 던진다.
     */
    public static DateRange of(LocalDate date, LocalDate from, LocalDate to) {
        if (date != null) {
            if (from != null || to != null) {
                throw new IllegalArgumentException("date 와 from·to 는 함께 보낼 수 없습니다.");
            }
            return new DateRange(date, date);
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("date 또는 from·to 를 모두 입력해 주세요.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from 은 to 보다 늦을 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new IllegalArgumentException("조회 기간은 최대 " + MAX_DAYS + "일입니다.");
        }
        return new DateRange(from, to);
    }
}
