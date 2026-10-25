package com.segye;

import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("애플리케이션 부팅 스모크 테스트")
class SegyeApplicationTests extends PostgresTestSupport {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("PostgreSQL 을 물고 스프링 컨텍스트가 뜨고, Flyway 가 전 엔티티의 테이블을 만든다")
    void contextLoads() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }

        // 컨텍스트가 떴다는 것 자체가 ddl-auto: validate 를 통과했다는 뜻이다.
        // 여기서는 그 스키마를 Hibernate 가 아니라 Flyway 마이그레이션이 만들었는지 확인한다.
        assertThat(tableNames()).contains("member", "category", "schedule", "todo", "account_record", "budget");
        assertThat(failedMigrations()).isZero();
        assertThat(appliedVersions()).contains("1");
    }

    private Integer failedMigrations() {
        return jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where not success", Integer.class);
    }

    private java.util.List<String> appliedVersions() {
        return jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success and version is not null", String.class);
    }

    @Test
    @DisplayName("Hibernate 가 PostgreSQL 방언을 선택한다")
    void usesPostgresDialect() {
        String version = jdbcTemplate.queryForObject("select version()", String.class);
        assertThat(version).startsWith("PostgreSQL");
    }

    private java.util.List<String> tableNames() {
        return jdbcTemplate.queryForList(
                "select tablename from pg_tables where schemaname = 'public'", String.class);
    }
}
