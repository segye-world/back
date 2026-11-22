package com.segye.schedule;

import com.segye.common.DateRange;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.schedule.dto.ScheduleDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository repo;
    private final MemberRepository memberRepo;

    public ScheduleService(ScheduleRepository repo, MemberRepository memberRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
    }

    public ScheduleDtos.ScheduleResponse create(Long memberId, ScheduleDtos.CreateRequest req) {
        MinuteRange range = MinuteRange.of(req.startMinute(), req.endMinute(), req.startHour(), req.endHour());
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new SecurityException("회원이 없습니다."));
        Schedule saved = repo.save(new Schedule(
                member, req.title(), req.date(), range.start(), range.end(), req.colorHex()
        ));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.ScheduleResponse> listByDate(Long memberId, LocalDate date) {
        return repo.findByMember_IdAndDateOrderByStartMinuteAsc(memberId, date)
                .stream().map(this::toDto).toList();
    }

    /** 구간 안의 일정을 날짜, 시작 분 순으로 돌려준다. */
    @Transactional(readOnly = true)
    public List<ScheduleDtos.ScheduleResponse> listRange(Long memberId, DateRange range) {
        return repo.findByMember_IdAndDateBetweenOrderByDateAscStartMinuteAsc(memberId, range.from(), range.to())
                .stream().map(this::toDto).toList();
    }

    public ScheduleDtos.ScheduleResponse update(Long memberId, Long id, ScheduleDtos.UpdateRequest req) {
        MinuteRange range = MinuteRange.of(req.startMinute(), req.endMinute(), req.startHour(), req.endHour());
        Schedule schedule = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 없습니다."));
        schedule.update(req.title(), range.start(), range.end(), req.colorHex());
        return toDto(schedule);
    }

    public void delete(Long memberId, Long id) {
        Schedule schedule = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 없습니다."));
        repo.delete(schedule);
    }

    private ScheduleDtos.ScheduleResponse toDto(Schedule s) {
        return new ScheduleDtos.ScheduleResponse(
                s.getId(), s.getTitle(), s.getDate(),
                s.getStartMinute(), s.getEndMinute(),
                s.getStartMinute() / 60, (s.getEndMinute() + 59) / 60,
                s.getColorHex()
        );
    }

    /** 요청의 분 값(없으면 시 값)을 검증된 분 범위로 바꾼다. */
    private record MinuteRange(int start, int end) {

        static MinuteRange of(Integer startMinute, Integer endMinute, Integer startHour, Integer endHour) {
            MinuteRange range;
            if (startMinute != null || endMinute != null) {
                if (startMinute == null || endMinute == null) {
                    throw new IllegalArgumentException("시작·종료 시간을 모두 입력해 주세요.");
                }
                range = new MinuteRange(startMinute, endMinute);
            } else if (startHour != null && endHour != null) {
                range = new MinuteRange(startHour * 60, endHour * 60);
            } else {
                throw new IllegalArgumentException("시작·종료 시간을 모두 입력해 주세요.");
            }
            if (range.start() >= range.end()) {
                throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야 합니다.");
            }
            return range;
        }
    }
}
