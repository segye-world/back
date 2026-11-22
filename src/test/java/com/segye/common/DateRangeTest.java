package com.segye.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("조회 날짜 구간")
class DateRangeTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 1);

    @Test
    @DisplayName("date 만 보내면 그 하루가 된다")
    void singleDate() {
        assertThat(DateRange.of(D, null, null)).isEqualTo(new DateRange(D, D));
    }

    @Test
    @DisplayName("from·to 를 보내면 양끝을 포함한 구간이 된다")
    void range() {
        assertThat(DateRange.of(null, D, D.plusDays(6))).isEqualTo(new DateRange(D, D.plusDays(6)));
    }

    @Test
    @DisplayName("최대 31일까지 허용하고 32일부터 거절한다")
    void maxDays() {
        assertThat(DateRange.of(null, D, D.plusDays(30)).to()).isEqualTo(D.plusDays(30));
        assertThatThrownBy(() -> DateRange.of(null, D, D.plusDays(31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조회 기간은 최대 31일입니다.");
    }

    @Test
    @DisplayName("from 이 to 보다 늦으면 거절한다")
    void reversed() {
        assertThatThrownBy(() -> DateRange.of(null, D.plusDays(1), D))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("값이 빠지거나 date 와 구간을 함께 보내면 거절한다")
    void missingOrMixed() {
        assertThatThrownBy(() -> DateRange.of(null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateRange.of(null, D, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateRange.of(D, D, D)).isInstanceOf(IllegalArgumentException.class);
    }
}
