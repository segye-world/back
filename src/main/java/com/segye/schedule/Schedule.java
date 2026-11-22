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

    /** 하루 시작(00:00)부터의 분. 0~1439. */
    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    /** 하루 시작(00:00)부터의 분. 1~1440 (1440 = 24:00). */
    @Column(name = "end_minute", nullable = false)
    private int endMinute;

    @Column(name = "color_hex", nullable = false, length = 20)
    private String colorHex;

    public Schedule(Member member, String title, LocalDate date,
                    int startMinute, int endMinute, String colorHex) {
        this.member = member;
        this.title = title;
        this.date = date;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.colorHex = colorHex;
    }

    public void update(String title, int startMinute, int endMinute, String colorHex) {
        this.title = title;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.colorHex = colorHex;
    }
}
