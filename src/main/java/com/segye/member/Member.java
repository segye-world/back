package com.segye.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_provider", columnNames = {"provider", "provider_id"})
})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원은 로그인 수단의 회원번호로 식별한다. 이메일은 카카오에서 받지 않는다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    // 카카오 회원번호(int64)를 문자열로 저장한다.
    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    // 동의하지 않았거나 카카오에서 비어 있을 수 있다.
    @Column(length = 100)
    private String nickname;

    public Member(AuthProvider provider, String providerId, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}
