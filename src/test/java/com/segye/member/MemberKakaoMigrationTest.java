package com.segye.member;

import com.segye.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V2 는 데이터를 옮기는 마이그레이션이라, 빈 DB 가 아니라 V1 시점의 행이 있는 DB 에서 검증한다.
 * 애플리케이션이 쓰는 public 스키마와 섞이지 않도록 별도 스키마에서 돌린다.
 */
@DisplayName("V2 마이그레이션: 이메일 회원 → 카카오 로그인 스키마")
class MemberKakaoMigrationTest extends PostgresTestSupport {

    private static final String SCHEMA = "v2_migration_check";

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Flyway flywayTo(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .target(target)
                .load();
    }

    @Test
    @DisplayName("기존 이메일 회원은 DEV 계정으로 남고, email·password 컬럼은 사라진다")
    void migratesExistingEmailMembers() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        flywayTo("1").migrate();
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO " + SCHEMA + ".member (email, password) VALUES ('segye@example.com', 'hash') RETURNING id",
                Long.class);

        flywayTo("2").migrate();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT provider, provider_id, nickname FROM " + SCHEMA + ".member WHERE id = ?", id);
        assertThat(row).containsEntry("provider", "DEV")
                .containsEntry("provider_id", "legacy-" + id)
                .containsEntry("nickname", "segye");

        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = 'member'",
                String.class, SCHEMA);
        assertThat(columns).doesNotContain("email", "password");

        jdbcTemplate.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
    }
}
