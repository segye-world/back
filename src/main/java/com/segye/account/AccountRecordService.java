package com.segye.account;

import com.segye.account.dto.AccountRecordDtos;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.paymentmethod.PaymentMethod;
import com.segye.paymentmethod.PaymentMethodRepository;
import com.segye.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class AccountRecordService {

    private final AccountRecordRepository repo;
    private final MemberRepository memberRepo;
    private final CategoryRepository categoryRepo;
    private final ScheduleRepository scheduleRepo;
    private final PaymentMethodRepository paymentMethodRepo;

    public AccountRecordService(AccountRecordRepository repo, MemberRepository memberRepo,
                                CategoryRepository categoryRepo, ScheduleRepository scheduleRepo,
                                PaymentMethodRepository paymentMethodRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.categoryRepo = categoryRepo;
        this.scheduleRepo = scheduleRepo;
        this.paymentMethodRepo = paymentMethodRepo;
    }

    public AccountRecordDtos.AccountRecordResponse create(Long memberId, AccountRecordDtos.CreateRequest req) {
        Member member = memberRepo.findById(memberId).orElseThrow(() -> new SecurityException("회원이 없습니다."));
        Category category = categoryRepo.findById(req.categoryId()).orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));

        if (req.scheduleId() != null) {
            scheduleRepo.findByIdAndMember_Id(req.scheduleId(), memberId)
                    .orElseThrow(() -> new IllegalArgumentException("연결할 일정이 없습니다."));
        }
        PaymentMethod paymentMethod = findPaymentMethod(memberId, req.paymentMethodId());

        AccountRecord saved = repo.save(new AccountRecord(member, category, paymentMethod,
                req.amount(), req.transactionTime(), req.scheduleId()));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountRecordDtos.AccountRecordResponse> listRange(Long memberId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        return repo.findByMember_IdAndTransactionTimeBetweenOrderByTransactionTimeAsc(memberId, start, endExclusive)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountRecordDtos.AccountRecordResponse> listMonthly(Long memberId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay();

        return repo.findByMember_IdAndTransactionTimeBetweenOrderByTransactionTimeAsc(memberId, start, endExclusive)
                .stream().map(this::toDto).toList();
    }

    public AccountRecordDtos.AccountRecordResponse update(Long memberId, Long id, AccountRecordDtos.UpdateRequest req) {
        AccountRecord ar = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("내역이 없습니다."));

        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));

        if (req.scheduleId() != null) {
            scheduleRepo.findByIdAndMember_Id(req.scheduleId(), memberId)
                    .orElseThrow(() -> new IllegalArgumentException("연결할 일정이 없습니다."));
        }
        PaymentMethod paymentMethod = findPaymentMethod(memberId, req.paymentMethodId());

        ar.update(category, paymentMethod, req.amount(), req.transactionTime(), req.scheduleId());
        return toDto(ar);
    }

    public void delete(Long memberId, Long id) {
        AccountRecord ar = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("내역이 없습니다."));
        repo.delete(ar);
    }

    // 남의 지출 수단을 붙이지 못하도록 소유자까지 함께 확인한다.
    private PaymentMethod findPaymentMethod(Long memberId, Long paymentMethodId) {
        if (paymentMethodId == null) return null;
        return paymentMethodRepo.findByIdAndMember_Id(paymentMethodId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("지출 수단이 없습니다."));
    }

    private AccountRecordDtos.AccountRecordResponse toDto(AccountRecord ar) {
        PaymentMethod pm = ar.getPaymentMethod();
        return new AccountRecordDtos.AccountRecordResponse(
                ar.getId(),
                ar.getCategory().getId(),
                ar.getCategory().getName(),
                ar.getCategory().getType().name(),
                pm != null ? pm.getId() : null,
                pm != null ? pm.getName() : null,
                ar.getAmount(),
                ar.getTransactionTime(),
                ar.getScheduleId()
        );
    }
}
