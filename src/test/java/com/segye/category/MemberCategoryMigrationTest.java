package com.segye.category;

import com.segye.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberCategoryMigrationTest extends PostgresTestSupport {

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @Test
    void migratesSharedCategoriesByRecordOwnerAndKeepsBothReferences() {
        String schema = "v3_category_migration_check";
        jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        try {
            migrate(schema, "2");
            Long first = insertMember(schema, "first");
            Long second = insertMember(schema, "second");
            Long noRecords = insertMember(schema, "no-records");
            Long expense = insertCategory(schema, "식비", "EXPENSE");
            Long source = insertCategory(schema, "현금", "INCOME");
            insertCategory(schema, "미사용 공용 항목", "EXPENSE");
            Long firstRecord = insertRecord(schema, first, expense, source);
            Long secondRecord = insertRecord(schema, second, expense, source);
            Long incomeRecord = insertRecord(schema, first, source, null);

            migrate(schema, "3");

            Map<String, Object> firstRow = record(schema, firstRecord);
            Map<String, Object> secondRow = record(schema, secondRecord);
            assertThat(firstRow).containsEntry("category_owner", first).containsEntry("source_owner", first)
                    .containsEntry("memo", "기존 거래").containsEntry("amount", -1000L);
            assertThat(secondRow).containsEntry("category_owner", second).containsEntry("source_owner", second);
            assertThat(secondRow.get("category_id")).isNotEqualTo(firstRow.get("category_id"));
            assertThat(secondRow.get("source_category_id")).isNotEqualTo(firstRow.get("source_category_id"));
            assertThat(record(schema, incomeRecord)).containsEntry("category_id", firstRow.get("source_category_id"))
                    .containsEntry("source_category_id", null);
            assertThat(jdbc.queryForList("SELECT name FROM " + schema + ".category WHERE member_id = ?", String.class, noRecords))
                    .containsExactlyInAnyOrder("월급", "현금", "부수입");
            assertThat(jdbc.queryForList("SELECT name FROM " + schema + ".category WHERE member_id = ? AND type = 'INCOME'", String.class, first))
                    .containsExactlyInAnyOrder("월급", "현금", "부수입");
            assertThat(jdbc.queryForList("SELECT name FROM " + schema + ".category", String.class))
                    .doesNotContain("미사용 공용 항목");
            assertThatThrownBy(() -> jdbc.update("UPDATE " + schema + ".account_record SET source_category_id = ? WHERE id = ?",
                    secondRow.get("source_category_id"), firstRecord)).isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private void migrate(String schema, String target) {
        Flyway.configure().dataSource(dataSource).schemas(schema).target(target).load().migrate();
    }

    private Long insertMember(String schema, String providerId) {
        return jdbc.queryForObject("INSERT INTO " + schema + ".member (provider, provider_id, nickname) VALUES ('DEV', ?, ?) RETURNING id",
                Long.class, providerId, providerId);
    }

    private Long insertCategory(String schema, String name, String type) {
        return jdbc.queryForObject("INSERT INTO " + schema + ".category (name, type) VALUES (?, ?) RETURNING id", Long.class, name, type);
    }

    private Long insertRecord(String schema, Long owner, Long category, Long source) {
        return jdbc.queryForObject("INSERT INTO " + schema + ".account_record "
                        + "(member_id, category_id, source_category_id, amount, transaction_time, memo) "
                        + "VALUES (?, ?, ?, -1000, CURRENT_TIMESTAMP, '기존 거래') RETURNING id",
                Long.class, owner, category, source);
    }

    private Map<String, Object> record(String schema, Long id) {
        return jdbc.queryForMap("""
                SELECT r.category_id, r.source_category_id, r.memo, r.amount,
                       c.member_id AS category_owner, s.member_id AS source_owner
                FROM %1$s.account_record r
                JOIN %1$s.category c ON c.id = r.category_id
                LEFT JOIN %1$s.category s ON s.id = r.source_category_id
                WHERE r.id = ?
                """.formatted(schema), id);
    }
}
