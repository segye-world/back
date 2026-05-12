package com.segye.todo;

import com.segye.member.Member;
import com.segye.schedule.Schedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "todo", indexes = {
        @Index(name = "idx_todo_member_date", columnList = "member_id,date")
})
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // schedule 삭제 시 DB 레벨에서 schedule_id → NULL 처리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Schedule schedule;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "is_done", nullable = false)
    private boolean isDone;

    @Column(nullable = false)
    private LocalDate date;

    public Todo(Member member, Schedule schedule, String label, LocalDate date) {
        this.member = member;
        this.schedule = schedule;
        this.label = label;
        this.isDone = false;
        this.date = date;
    }

    public void update(String label, Boolean isDone) {
        if (label != null) this.label = label;
        if (isDone != null) this.isDone = isDone;
    }
}
