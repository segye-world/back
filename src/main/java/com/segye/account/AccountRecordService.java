package com.segye.account;

import com.segye.account.dto.AccountRecordDtos;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
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

    public AccountRecordService(AccountRecordRepository repo, MemberRepository memberRepo,
                                CategoryRepository categoryRepo, ScheduleRepository scheduleRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.categoryRepo = categoryRepo;
        this.scheduleRepo = scheduleRepo;
    }

    public AccountRecordDtos.AccountRecordResponse create(Long memberId, AccountRecordDtos.CreateRequest req) {
        Member member = memberRepo.findById(memberId).orElseThrow(() -> new SecurityException("회원이 없습니다."));
        Category category = categoryRepo.findById(req.categoryId()).orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));

        if (req.scheduleId() != null) {
            scheduleRepo.findByIdAndMember_Id(req.scheduleId(), memberId)
                    .orElseThrow(() -> new IllegalArgumentException("연결할 일정이 없습니다."));
        }
        AccountRecord saved = repo.save(new AccountRecord(member, category, req.amount(), req.transactionTime(), req.scheduleId()));
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

        ar.update(category, req.amount(), req.transactionTime(), req.scheduleId());
        return toDto(ar);
    }

    public void delete(Long memberId, Long id) {
        AccountRecord ar = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("내역이 없습니다."));
        repo.delete(ar);
    }

    private AccountRecordDtos.AccountRecordResponse toDto(AccountRecord ar) {
        return new AccountRecordDtos.AccountRecordResponse(
                ar.getId(),
                ar.getCategory().getId(),
                ar.getCategory().getName(),
                ar.getCategory().getType().name(),
                ar.getAmount(),
                ar.getTransactionTime(),
                ar.getScheduleId()
        );
    }
}
