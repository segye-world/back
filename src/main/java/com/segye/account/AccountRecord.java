package com.segye.account;

import com.segye.category.Category;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "account_record", indexes = {
        @Index(name = "idx_ar_member_time", columnList = "member_id,transaction_time")
})
public class AccountRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 카테고리
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 지출이 빠져나간 수입원(=지출 수단). 수입원 카테고리(Category, type=INCOME)를 그대로 가리킨다.
    // 수입 기록에는 없을 수 있어 nullable 이며, 수입원이 삭제되어도 기록은 남도록
    // DB 레벨에서 source_category_id → NULL 처리한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_category_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Category sourceCategory;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    // ERD상 schedule_id nullable: 이번 MVP에서는 엔티티만 nullable Long으로 두고, 나중에 Schedule 엔티티 붙일 때 연관관계로 바꾸면 됨
    @Column(name = "schedule_id")
    private Long scheduleId;

    public AccountRecord(Member member, Category category, Category sourceCategory,
                         Long amount, LocalDateTime transactionTime, Long scheduleId) {
        this.member = member;
        this.category = category;
        this.sourceCategory = sourceCategory;
        this.amount = amount;
        this.transactionTime = transactionTime;
        this.scheduleId = scheduleId;
    }

    public void update(Category category, Category sourceCategory,
                       Long amount, LocalDateTime transactionTime, Long scheduleId) {
        this.category = category;
        this.sourceCategory = sourceCategory;
        this.amount = amount;
        this.transactionTime = transactionTime;
        this.scheduleId = scheduleId;
    }
}
