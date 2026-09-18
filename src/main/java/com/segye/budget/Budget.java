package com.segye.budget;

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

@Getter
@NoArgsConstructor
@Entity
// 회원별로 한 달에 하나의 한도만 두도록 (member_id, year, month) 를 유니크로 묶는다.
@Table(name = "budget", indexes = {
        @Index(name = "idx_budget_member_year_month", columnList = "member_id,year,month", unique = true)
})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "limit_amount", nullable = false)
    private Long limitAmount;

    public Budget(Member member, int year, int month, Long limitAmount) {
        this.member = member;
        this.year = year;
        this.month = month;
        this.limitAmount = limitAmount;
    }

    public void update(Long limitAmount) {
        this.limitAmount = limitAmount;
    }
}
