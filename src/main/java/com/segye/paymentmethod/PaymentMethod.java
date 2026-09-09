package com.segye.paymentmethod;

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
// 같은 회원이 같은 이름의 수단을 두 번 만들지 못하도록 (member_id, name) 을 유니크로 묶는다.
// 유니크 제약은 Member 와 같은 방식으로 @Table 인덱스가 담당한다.
@Table(name = "payment_method", indexes = {
        @Index(name = "idx_pm_member_name", columnList = "member_id,name", unique = true)
})
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리와 달리 지출 수단은 회원별로 관리한다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    public PaymentMethod(Member member, String name) {
        this.member = member;
        this.name = name;
    }

    public void update(String name) {
        this.name = name;
    }
}
