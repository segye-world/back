package com.segye.schedule;

import com.segye.account.AccountRecord;
import com.segye.account.AccountRecordRepository;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.category.CategoryType;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.schedule.dto.ScheduleDtos;
import com.segye.support.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AccountRecordRepository accountRecordRepository;

    @Autowired
    EntityManager entityManager;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(
                new Member(AuthProvider.DEV, "schedule-" + UUID.randomUUID(), "tester"));
        memberId = member.getId();
    }

    private static final LocalDate DAY = LocalDate.of(2026, 9, 8);

    /** 분 단위 요청 (00:00부터의 분) */
    private ScheduleDtos.CreateRequest request(int startMinute, int endMinute) {
        return new ScheduleDtos.CreateRequest("아침 운동", DAY, startMinute, endMinute, null, null, "#F3A3A4");
    }

    /** 분 단위를 모르는 기존 앱의 시 단위 요청 */
    private ScheduleDtos.CreateRequest hourRequest(int startHour, int endHour) {
        return new ScheduleDtos.CreateRequest("아침 운동", DAY, null, null, startHour, endHour, "#F3A3A4");
    }

    @Test
    @DisplayName("분 단위 일정이 그대로 저장되고 조회된다")
    void createsScheduleInMinutes() {
        ScheduleDtos.ScheduleResponse created = scheduleService.create(memberId, request(14 * 60 + 30, 16 * 60 + 10));

        assertThat(created.id()).isNotNull();
        assertThat(created.startMinute()).isEqualTo(870);
        assertThat(created.endMinute()).isEqualTo(970);

        List<ScheduleDtos.ScheduleResponse> found = scheduleService.listByDate(memberId, DAY);
        assertThat(found).extracting(ScheduleDtos.ScheduleResponse::id).containsExactly(created.id());
    }

    @Test
    @DisplayName("응답의 시 단위 값은 시작은 내림, 종료는 올림한다")
    void respondsLegacyHoursCoveringTheSchedule() {
        ScheduleDtos.ScheduleResponse created = scheduleService.create(memberId, request(14 * 60 + 30, 16 * 60 + 10));

        assertThat(created.startHour()).isEqualTo(14);
        assertThat(created.endHour()).isEqualTo(17);
    }

    @Test
    @DisplayName("시 단위로만 보낸 요청은 분으로 바꿔 저장한다")
    void acceptsLegacyHourRequest() {
        ScheduleDtos.ScheduleResponse created = scheduleService.create(memberId, hourRequest(7, 24));

        assertThat(created.startMinute()).isEqualTo(7 * 60);
        assertThat(created.endMinute()).isEqualTo(1440);
        assertThat(created.startHour()).isEqualTo(7);
        assertThat(created.endHour()).isEqualTo(24);
    }

    @Test
    @DisplayName("같은 날 일정은 시작 분 순서로 조회된다")
    void listsByStartMinute() {
        Long later = scheduleService.create(memberId, request(9 * 60 + 40, 10 * 60)).id();
        Long earlier = scheduleService.create(memberId, request(9 * 60 + 10, 9 * 60 + 30)).id();

        assertThat(scheduleService.listByDate(memberId, DAY))
                .extracting(ScheduleDtos.ScheduleResponse::id)
                .containsExactly(earlier, later);
    }

    @ParameterizedTest(name = "start={0}, end={1}")
    @CsvSource({"540, 540", "600, 590", "1380, 60"})
    @DisplayName("시작 시간이 종료 시간보다 늦거나 같으면 생성이 거절된다")
    void rejectsInvalidRangeOnCreate(int startMinute, int endMinute) {
        assertThatThrownBy(() -> scheduleService.create(memberId, request(startMinute, endMinute)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 시간은 종료 시간보다 빨라야 합니다.");
    }

    @Test
    @DisplayName("시작·종료 중 하나만 보내면 거절된다")
    void rejectsMissingTime() {
        assertThatThrownBy(() -> scheduleService.create(memberId,
                new ScheduleDtos.CreateRequest("아침 운동", DAY, 420, null, null, null, "#F3A3A4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작·종료 시간을 모두 입력해 주세요.");
        assertThatThrownBy(() -> scheduleService.create(memberId,
                new ScheduleDtos.CreateRequest("아침 운동", DAY, null, null, null, null, "#F3A3A4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작·종료 시간을 모두 입력해 주세요.");
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 수정도 거절된다")
    void rejectsInvalidRangeOnUpdate() {
        Long id = scheduleService.create(memberId, request(7 * 60, 9 * 60)).id();

        assertThatThrownBy(() -> scheduleService.update(memberId, id,
                new ScheduleDtos.UpdateRequest("수정", 11 * 60, 10 * 60, null, null, "#000000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 시간은 종료 시간보다 빨라야 합니다.");
    }

    @Test
    @DisplayName("분 단위로 수정할 수 있다")
    void updatesInMinutes() {
        Long id = scheduleService.create(memberId, request(7 * 60, 9 * 60)).id();

        ScheduleDtos.ScheduleResponse updated = scheduleService.update(memberId, id,
                new ScheduleDtos.UpdateRequest("수정", 7 * 60 + 15, 8 * 60 + 45, null, null, "#000000"));

        assertThat(updated.startMinute()).isEqualTo(435);
        assertThat(updated.endMinute()).isEqualTo(525);
    }

    @Test
    @DisplayName("다른 회원의 일정은 조회/수정/삭제할 수 없다")
    void cannotTouchOtherMembersSchedule() {
        Long id = scheduleService.create(memberId, request(7 * 60, 9 * 60)).id();
        Long otherId = memberRepository.save(
                new Member(AuthProvider.DEV, "other-" + UUID.randomUUID(), "tester")).getId();

        assertThat(scheduleService.listByDate(otherId, LocalDate.of(2026, 9, 8))).isEmpty();
        assertThatThrownBy(() -> scheduleService.delete(otherId, id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("일정을 삭제해도 연결된 수입·지출 기록은 남고 연결만 끊어진다")
    void deletingScheduleDisconnectsAccountRecordsInsteadOfLeavingDanglingId() {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Category category = categoryRepository.save(new Category(member, "식비", CategoryType.EXPENSE));
        Long scheduleId = scheduleService.create(memberId, request(7 * 60, 9 * 60)).id();
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();

        AccountRecord record = accountRecordRepository.save(new AccountRecord(
                member, category, null, -12900L, LocalDateTime.now(), schedule, null));
        Long recordId = record.getId();

        scheduleService.delete(memberId, scheduleId);
        entityManager.flush();
        entityManager.clear();

        AccountRecord reloaded = accountRecordRepository.findById(recordId).orElseThrow();
        assertThat(reloaded.getSchedule()).isNull();
        assertThat(scheduleRepository.findById(scheduleId)).isEmpty();
    }
}
