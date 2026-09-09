package com.segye.paymentmethod;

import com.segye.account.AccountRecordService;
import com.segye.account.dto.AccountRecordDtos;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.category.CategoryType;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.paymentmethod.dto.PaymentMethodDtos;
import com.segye.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("지출 수단 서비스")
class PaymentMethodServiceTest extends PostgresTestSupport {

    @Autowired
    PaymentMethodService paymentMethodService;

    @Autowired
    AccountRecordService accountRecordService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    private Long memberId;
    private Long otherMemberId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        memberId = newMember().getId();
        otherMemberId = newMember().getId();
        Category category = categoryRepository.save(new Category("식비", CategoryType.EXPENSE));
        categoryId = category.getId();
    }

    private Member newMember() {
        return memberRepository.save(new Member("pm-" + UUID.randomUUID() + "@example.com", "hash"));
    }

    private PaymentMethodDtos.UpsertRequest request(String name) {
        return new PaymentMethodDtos.UpsertRequest(name);
    }

    @Test
    @DisplayName("지출 수단을 만들고 회원별로 조회한다")
    void createsAndLists() {
        PaymentMethodDtos.PaymentMethodResponse created = paymentMethodService.create(memberId, request("현금"));

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("현금");

        assertThat(paymentMethodService.list(memberId))
                .extracting(PaymentMethodDtos.PaymentMethodResponse::name)
                .containsExactly("현금");
        assertThat(paymentMethodService.list(otherMemberId)).isEmpty();
    }

    @Test
    @DisplayName("이름 앞뒤 공백은 제거하고 저장한다")
    void trimsName() {
        assertThat(paymentMethodService.create(memberId, request("  신용카드  ")).name())
                .isEqualTo("신용카드");
    }

    @Test
    @DisplayName("같은 회원이 같은 이름을 두 번 만들 수 없다")
    void rejectsDuplicateNameForSameMember() {
        paymentMethodService.create(memberId, request("현금"));

        assertThatThrownBy(() -> paymentMethodService.create(memberId, request("현금")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 있는 지출 수단입니다.");
    }

    @Test
    @DisplayName("다른 회원은 같은 이름을 쓸 수 있다")
    void allowsSameNameForDifferentMember() {
        paymentMethodService.create(memberId, request("현금"));

        assertThat(paymentMethodService.create(otherMemberId, request("현금")).name()).isEqualTo("현금");
    }

    @Test
    @DisplayName("이름을 그대로 다시 보내는 수정은 중복으로 보지 않는다")
    void allowsUpdateWithUnchangedName() {
        Long id = paymentMethodService.create(memberId, request("현금")).id();

        assertThat(paymentMethodService.update(memberId, id, request("현금")).name()).isEqualTo("현금");
    }

    @Test
    @DisplayName("다른 회원의 지출 수단은 수정/삭제할 수 없다")
    void cannotTouchOtherMembersPaymentMethod() {
        Long id = paymentMethodService.create(memberId, request("현금")).id();

        assertThatThrownBy(() -> paymentMethodService.update(otherMemberId, id, request("카드")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지출 수단이 없습니다.");
        assertThatThrownBy(() -> paymentMethodService.delete(otherMemberId, id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지출 수단이 없습니다.");
    }

    @Test
    @DisplayName("가계부 기록에 지출 수단을 연결하면 이름까지 함께 응답한다")
    void linksPaymentMethodToAccountRecord() {
        Long paymentMethodId = paymentMethodService.create(memberId, request("신용카드")).id();

        AccountRecordDtos.AccountRecordResponse created = accountRecordService.create(memberId,
                new AccountRecordDtos.CreateRequest(categoryId, -12900L,
                        LocalDate.of(2026, 9, 8).atStartOfDay(), null, paymentMethodId));

        assertThat(created.paymentMethodId()).isEqualTo(paymentMethodId);
        assertThat(created.paymentMethodName()).isEqualTo("신용카드");
    }

    @Test
    @DisplayName("남의 지출 수단은 가계부 기록에 연결할 수 없다")
    void rejectsOtherMembersPaymentMethodOnRecord() {
        Long otherPaymentMethodId = paymentMethodService.create(otherMemberId, request("현금")).id();

        assertThatThrownBy(() -> accountRecordService.create(memberId,
                new AccountRecordDtos.CreateRequest(categoryId, -1000L,
                        LocalDateTime.of(2026, 9, 8, 12, 0), null, otherPaymentMethodId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지출 수단이 없습니다.");
    }

    @Test
    @DisplayName("지출 수단 없이도 가계부 기록을 만들 수 있다")
    void allowsRecordWithoutPaymentMethod() {
        AccountRecordDtos.AccountRecordResponse created = accountRecordService.create(memberId,
                new AccountRecordDtos.CreateRequest(categoryId, 280000L,
                        LocalDateTime.of(2026, 9, 8, 9, 0), null, null));

        assertThat(created.paymentMethodId()).isNull();
        assertThat(created.paymentMethodName()).isNull();
    }

    @Test
    @DisplayName("수정으로 지출 수단 연결을 해제할 수 있다")
    void unlinksPaymentMethodOnUpdate() {
        Long paymentMethodId = paymentMethodService.create(memberId, request("현금")).id();
        Long recordId = accountRecordService.create(memberId,
                new AccountRecordDtos.CreateRequest(categoryId, -5000L,
                        LocalDateTime.of(2026, 9, 8, 15, 0), null, paymentMethodId)).id();

        AccountRecordDtos.AccountRecordResponse updated = accountRecordService.update(memberId, recordId,
                new AccountRecordDtos.UpdateRequest(categoryId, -5000L,
                        LocalDateTime.of(2026, 9, 8, 15, 0), null, null));

        assertThat(updated.paymentMethodId()).isNull();
    }

    @Test
    @DisplayName("삭제한 지출 수단은 목록에서 사라진다")
    void deletes() {
        Long id = paymentMethodService.create(memberId, request("현금")).id();

        paymentMethodService.delete(memberId, id);

        List<PaymentMethodDtos.PaymentMethodResponse> remaining = paymentMethodService.list(memberId);
        assertThat(remaining).isEmpty();
    }
}
