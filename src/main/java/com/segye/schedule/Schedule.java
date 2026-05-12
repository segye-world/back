package com.segye.schedule;

import com.segye.member.Member;
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

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "schedule", indexes = {
        @Index(name = "idx_schedule_member_date", columnList = "member_id,date")
})
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_hour", nullable = false)
    private int startHour;

    @Column(name = "end_hour", nullable = false)
    private int endHour;

    @Column(name = "color_hex", nullable = false, length = 20)
    private String colorHex;

    public Schedule(Member member, String title, LocalDate date,
                    int startHour, int endHour, String colorHex) {
        this.member = member;
        this.title = title;
        this.date = date;
        this.startHour = startHour;
        this.endHour = endHour;
        this.colorHex = colorHex;
    }

    public void update(String title, int startHour, int endHour, String colorHex) {
        this.title = title;
        this.startHour = startHour;
        this.endHour = endHour;
        this.colorHex = colorHex;
    }
}
