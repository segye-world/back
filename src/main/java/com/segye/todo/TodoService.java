package com.segye.todo;

import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.schedule.Schedule;
import com.segye.schedule.ScheduleRepository;
import com.segye.todo.dto.TodoDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TodoService {

    private final TodoRepository repo;
    private final MemberRepository memberRepo;
    private final ScheduleRepository scheduleRepo;

    public TodoService(TodoRepository repo, MemberRepository memberRepo,
                       ScheduleRepository scheduleRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.scheduleRepo = scheduleRepo;
    }

    public TodoDtos.TodoResponse create(Long memberId, TodoDtos.CreateRequest req) {
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new SecurityException("회원이 없습니다."));

        Schedule schedule = null;
        if (req.scheduleId() != null) {
            schedule = scheduleRepo.findByIdAndMember_Id(req.scheduleId(), memberId)
                    .orElseThrow(() -> new IllegalArgumentException("일정이 없습니다."));
        }

        Todo saved = repo.save(new Todo(member, schedule, req.label(), req.date()));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TodoDtos.TodoResponse> listByDate(Long memberId, LocalDate date) {
        return repo.findByMember_IdAndDateOrderByIdAsc(memberId, date)
                .stream().map(this::toDto).toList();
    }

    public TodoDtos.TodoResponse update(Long memberId, Long id, TodoDtos.UpdateRequest req) {
        Todo todo = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("할 일이 없습니다."));
        todo.update(req.label(), req.isDone());
        return toDto(todo);
    }

    public void delete(Long memberId, Long id) {
        Todo todo = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("할 일이 없습니다."));
        repo.delete(todo);
    }

    private TodoDtos.TodoResponse toDto(Todo t) {
        return new TodoDtos.TodoResponse(
                t.getId(),
                t.getSchedule() != null ? t.getSchedule().getId() : null,
                t.getLabel(),
                t.isDone(),
                t.getDate()
        );
    }
}
