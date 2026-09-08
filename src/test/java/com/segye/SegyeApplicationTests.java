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
    @DisplayName("PostgreSQL 을 물고 스프링 컨텍스트가 뜨고, 전 엔티티의 DDL 이 생성된다")
    void contextLoads() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }

        // ddl-auto: update 가 실제 PostgreSQL 에서 모든 테이블을 만들어냈는지 확인한다.
        assertThat(tableNames()).contains("member", "category", "schedule", "todo", "account_record");
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
