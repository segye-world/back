package com.segye.member;

import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("member 테이블 스키마")
class MemberSchemaTest extends PostgresTestSupport {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("같은 로그인 수단의 같은 회원번호는 DB 레벨에서 거부된다")
    @Transactional
    void rejectsDuplicateProviderIdAtDbLevel() {
        String kakaoId = "dup-" + UUID.randomUUID();
        memberRepository.saveAndFlush(new Member(AuthProvider.KAKAO, kakaoId, "a"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(new Member(AuthProvider.KAKAO, kakaoId, "b")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("회원번호가 같아도 로그인 수단이 다르면 다른 회원이다")
    @Transactional
    void sameProviderIdOnDifferentProviderIsAllowed() {
        String id = "same-" + UUID.randomUUID();
        memberRepository.saveAndFlush(new Member(AuthProvider.KAKAO, id, "a"));
        memberRepository.saveAndFlush(new Member(AuthProvider.DEV, id, "b"));

        assertThat(memberRepository.findByProviderAndProviderId(AuthProvider.KAKAO, id)).isPresent();
        assertThat(memberRepository.findByProviderAndProviderId(AuthProvider.DEV, id)).isPresent();
    }

    @Test
    @DisplayName("이메일·비밀번호 컬럼은 없고, provider·provider_id 는 NOT NULL, nickname 은 nullable 이다")
    void columns() {
        List<String> spec = jdbcTemplate.query(
                """
                select column_name || '|' || data_type || '|' || coalesce(character_maximum_length::text, '-')
                       || '|' || is_nullable
                from information_schema.columns
                where table_schema = 'public' and table_name = 'member' and column_name <> 'id'
                order by column_name
                """,
                (rs, n) -> rs.getString(1));

        assertThat(spec).containsExactly(
                "nickname|character varying|100|YES",
                "provider|character varying|20|NO",
                "provider_id|character varying|64|NO");
    }
}
