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

    // email 유니크 인덱스를 세는 쿼리.
    //
    // 스키마는 Flyway(V1__baseline.sql)가 만들므로, 이 테스트는 마이그레이션 스크립트가
    // "email 유니크 인덱스는 정확히 하나"라는 스키마 계약을 지키는지 고정하는 역할을 한다.
    private static final String UNIQUE_INDEXES_ON_EMAIL = """
            select i.relname
            from pg_index x
            join pg_class i on i.oid = x.indexrelid
            join pg_class t on t.oid = x.indrelid
            join pg_attribute a on a.attrelid = t.oid and a.attnum = any(x.indkey)
            where t.relname = 'member' and a.attname = 'email' and x.indisunique
            """;

    @Test
    @DisplayName("email 유니크 인덱스는 idx_member_email 하나만 존재한다")
    void hasExactlyOneUniqueIndexOnEmail() {
        List<String> indexes = jdbcTemplate.queryForList(UNIQUE_INDEXES_ON_EMAIL, String.class);

        assertThat(indexes).containsExactly("idx_member_email");
    }

    @Test
    @DisplayName("중복 이메일은 DB 레벨에서 거부된다")
    @Transactional
    void rejectsDuplicateEmailAtDbLevel() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        memberRepository.saveAndFlush(new Member(email, "hash"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(new Member(email, "hash2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("email/password 는 NOT NULL 이고 varchar(255) 로 매핑된다")
    void columnsAreNotNullVarchar255() {
        List<String> spec = jdbcTemplate.query(
                """
                select column_name || '|' || data_type || '|' || coalesce(character_maximum_length::text, '-')
                       || '|' || is_nullable
                from information_schema.columns
                where table_name = 'member' and column_name in ('email', 'password')
                order by column_name
                """,
                (rs, n) -> rs.getString(1));

        assertThat(spec).containsExactly(
                "email|character varying|255|NO",
                "password|character varying|255|NO");
    }
}
