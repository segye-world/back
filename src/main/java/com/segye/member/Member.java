package com.segye.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "member", indexes = {
        @Index(name = "idx_member_email", columnList = "email", unique = true)
})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유니크 제약은 위 @Table 의 idx_member_email 인덱스가 담당한다. @Column 에도 두면
    // Hibernate 가 부팅마다 중복 제약을 정리하려 들어 WARN 이 찍힌다.
    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password; // BCrypt hash

    public Member(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
