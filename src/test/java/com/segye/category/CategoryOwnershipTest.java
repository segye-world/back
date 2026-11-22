package com.segye.category;

import com.segye.account.AccountRecordService;
import com.segye.account.dto.AccountRecordDtos.CreateRequest;
import com.segye.account.dto.AccountRecordDtos.UpdateRequest;
import com.segye.auth.JwtTokenProvider;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.support.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class CategoryOwnershipTest extends PostgresTestSupport {

    private static final String PATH = "/api/v1/categories";

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider tokens;
    @Autowired MemberRepository members;
    @Autowired CategoryRepository categories;
    @Autowired CategoryService categoryService;
    @Autowired AccountRecordService records;
    @Autowired EntityManager entityManager;

    private Long memberId;
    private Long expenseId;
    private Long sourceId;
    private Long otherCategoryId;

    @BeforeEach
    void setUp() {
        Member member = members.save(new Member(AuthProvider.DEV, "owner-" + UUID.randomUUID(), "회원"));
        Member other = members.save(new Member(AuthProvider.DEV, "other-" + UUID.randomUUID(), "다른 회원"));
        memberId = member.getId();
        expenseId = categories.save(new Category(member, "식비", CategoryType.EXPENSE)).getId();
        sourceId = categories.save(new Category(member, "현금", CategoryType.INCOME)).getId();
        otherCategoryId = categories.save(new Category(other, "다른 수입원", CategoryType.INCOME)).getId();
    }

    @Test
    void categoryApiRequiresAuthenticationAndScopesRequestsToTheMember() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());

        String bearer = "Bearer " + tokens.createAccessToken(memberId);
        mvc.perform(get(PATH).param("type", "INCOME").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("현금"));
        mvc.perform(put(PATH + "/{id}", otherCategoryId).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"변경 시도","type":"INCOME"}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(PATH + "/{id}", otherCategoryId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isBadRequest());
        assertThat(categories.findById(otherCategoryId).orElseThrow().getName()).isEqualTo("다른 수입원");
    }

    @Test
    void creatingAndUpdatingRecordsRejectsAnotherMembersCategories() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 22, 10, 0);
        Long recordId = records.create(memberId, new CreateRequest(expenseId, -1000L, time, null, sourceId, null)).id();

        assertThatThrownBy(() -> records.create(memberId, new CreateRequest(otherCategoryId, 1000L, time, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> records.create(memberId, new CreateRequest(expenseId, -1000L, time, null, otherCategoryId, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> records.update(memberId, recordId,
                new UpdateRequest(otherCategoryId, 1000L, time, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> records.update(memberId, recordId,
                new UpdateRequest(expenseId, -1000L, time, null, otherCategoryId, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletingSourceOnlyCategoryKeepsTheRecordAndDeletingMainCategoryIsRejected() {
        LocalDate day = LocalDate.of(2026, 11, 22);
        Long recordId = records.create(memberId,
                new CreateRequest(expenseId, -1000L, day.atTime(10, 0), null, sourceId, "메모")).id();

        categoryService.delete(memberId, sourceId);
        entityManager.clear();
        assertThat(records.listRange(memberId, day, day)).singleElement().satisfies(record -> {
            assertThat(record.id()).isEqualTo(recordId);
            assertThat(record.categoryId()).isEqualTo(expenseId);
            assertThat(record.sourceCategoryId()).isNull();
            assertThat(record.memo()).isEqualTo("메모");
        });

        assertThatThrownBy(() -> categoryService.delete(memberId, expenseId))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("사용 중인");
    }
}
