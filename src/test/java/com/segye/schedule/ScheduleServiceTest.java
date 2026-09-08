package com.segye.schedule;

import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.schedule.dto.ScheduleDtos;
import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("일정 서비스")
class ScheduleServiceTest extends PostgresTestSupport {

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(
                new Member("schedule-" + UUID.randomUUID() + "@example.com", "hash"));
        memberId = member.getId();
    }

    private ScheduleDtos.CreateRequest request(int startHour, int endHour) {
        return new ScheduleDtos.CreateRequest("아침 운동", LocalDate.of(2026, 9, 8), startHour, endHour, "#F3A3A4");
    }

    @Test
    @DisplayName("정상 범위의 일정은 저장되고 조회된다")
    void createsSchedule() {
        ScheduleDtos.ScheduleResponse created = scheduleService.create(memberId, request(7, 9));

        assertThat(created.id()).isNotNull();
        assertThat(created.startHour()).isEqualTo(7);
        assertThat(created.endHour()).isEqualTo(9);

        List<ScheduleDtos.ScheduleResponse> found =
                scheduleService.listByDate(memberId, LocalDate.of(2026, 9, 8));
        assertThat(found).extracting(ScheduleDtos.ScheduleResponse::id).containsExactly(created.id());
    }

    @ParameterizedTest(name = "start={0}, end={1}")
    @CsvSource({"9, 9", "10, 9", "23, 1"})
    @DisplayName("시작 시간이 종료 시간보다 늦거나 같으면 생성이 거절된다")
    void rejectsInvalidHourRangeOnCreate(int startHour, int endHour) {
        assertThatThrownBy(() -> scheduleService.create(memberId, request(startHour, endHour)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 시간은 종료 시간보다 빨라야 합니다.");
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 수정도 거절된다")
    void rejectsInvalidHourRangeOnUpdate() {
        Long id = scheduleService.create(memberId, request(7, 9)).id();

        assertThatThrownBy(() -> scheduleService.update(memberId, id,
                new ScheduleDtos.UpdateRequest("수정", 11, 10, "#000000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 시간은 종료 시간보다 빨라야 합니다.");
    }

    @Test
    @DisplayName("다른 회원의 일정은 조회/수정/삭제할 수 없다")
    void cannotTouchOtherMembersSchedule() {
        Long id = scheduleService.create(memberId, request(7, 9)).id();
        Long otherId = memberRepository.save(
                new Member("other-" + UUID.randomUUID() + "@example.com", "hash")).getId();

        assertThat(scheduleService.listByDate(otherId, LocalDate.of(2026, 9, 8))).isEmpty();
        assertThatThrownBy(() -> scheduleService.delete(otherId, id))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
