package com.segye.schedule;

import com.segye.common.DateRange;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.schedule.dto.ScheduleDtos;
import com.segye.support.PostgresTestSupport;
import com.segye.todo.TodoService;
import com.segye.todo.dto.TodoDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("일정·할 일 기간 조회")
class RangeQueryTest extends PostgresTestSupport {

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    TodoService todoService;

    @Autowired
    MemberRepository memberRepository;

    private static final LocalDate MON = LocalDate.of(2026, 11, 2);

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = memberRepository.save(
                new Member(AuthProvider.DEV, "range-" + UUID.randomUUID(), "tester")).getId();
    }

    private Long schedule(Long owner, LocalDate date, int startMinute) {
        return scheduleService.create(owner, new ScheduleDtos.CreateRequest(
                "일정", date, startMinute, startMinute + 30, null, null, "#5D688A")).id();
    }

    private Long todo(Long owner, LocalDate date) {
        return todoService.create(owner, new TodoDtos.CreateRequest("할 일", date, null)).id();
    }

    @Test
    @DisplayName("일정은 구간 양끝을 포함하고 날짜, 시작 분 순으로 온다")
    void schedulesInRangeOrdered() {
        Long before = schedule(memberId, MON.minusDays(1), 600);
        Long tueLate = schedule(memberId, MON.plusDays(1), 900);
        Long monLate = schedule(memberId, MON, 840);
        Long tueEarly = schedule(memberId, MON.plusDays(1), 420);
        Long monEarly = schedule(memberId, MON, 60);
        Long wed = schedule(memberId, MON.plusDays(2), 0);
        Long after = schedule(memberId, MON.plusDays(3), 600);

        assertThat(scheduleService.listRange(memberId, new DateRange(MON, MON.plusDays(2))))
                .extracting(ScheduleDtos.ScheduleResponse::id)
                .containsExactly(monEarly, monLate, tueEarly, tueLate, wed)
                .doesNotContain(before, after);
    }

    @Test
    @DisplayName("할 일은 구간 양끝을 포함하고 날짜, 등록 순으로 온다")
    void todosInRangeOrdered() {
        Long tueFirst = todo(memberId, MON.plusDays(1));
        Long mon = todo(memberId, MON);
        Long tueSecond = todo(memberId, MON.plusDays(1));
        Long after = todo(memberId, MON.plusDays(2));

        assertThat(todoService.listRange(memberId, new DateRange(MON, MON.plusDays(1))))
                .extracting(TodoDtos.TodoResponse::id)
                .containsExactly(mon, tueFirst, tueSecond)
                .doesNotContain(after);
    }

    @Test
    @DisplayName("다른 회원의 일정·할 일은 구간 조회에 섞이지 않는다")
    void excludesOtherMembers() {
        Long otherId = memberRepository.save(
                new Member(AuthProvider.DEV, "range-other-" + UUID.randomUUID(), "tester")).getId();
        schedule(otherId, MON, 600);
        todo(otherId, MON);

        DateRange week = new DateRange(MON, MON.plusDays(6));
        assertThat(scheduleService.listRange(memberId, week)).isEmpty();
        assertThat(todoService.listRange(memberId, week)).isEmpty();
    }
}
