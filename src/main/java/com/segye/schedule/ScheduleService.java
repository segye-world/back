package com.segye.schedule;

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
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new SecurityException("회원이 없습니다."));
        Schedule saved = repo.save(new Schedule(
                member, req.title(), req.date(), req.startHour(), req.endHour(), req.colorHex()
        ));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.ScheduleResponse> listByDate(Long memberId, LocalDate date) {
        return repo.findByMember_IdAndDateOrderByStartHourAsc(memberId, date)
                .stream().map(this::toDto).toList();
    }

    public ScheduleDtos.ScheduleResponse update(Long memberId, Long id, ScheduleDtos.UpdateRequest req) {
        Schedule schedule = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 없습니다."));
        schedule.update(req.title(), req.startHour(), req.endHour(), req.colorHex());
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
                s.getStartHour(), s.getEndHour(), s.getColorHex()
        );
    }
}
