package com.segye.account;

import com.segye.account.dto.AccountRecordDtos;
import com.segye.budget.Budget;
import com.segye.budget.BudgetRepository;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.category.CategoryType;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountRecordService {

    private static final int RECENT_TRANSACTIONS_LIMIT = 5;

    private final AccountRecordRepository repo;
    private final MemberRepository memberRepo;
    private final CategoryRepository categoryRepo;
    private final ScheduleRepository scheduleRepo;
    private final BudgetRepository budgetRepo;

    public AccountRecordService(AccountRecordRepository repo, MemberRepository memberRepo,
                                CategoryRepository categoryRepo, ScheduleRepository scheduleRepo,
                                BudgetRepository budgetRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.categoryRepo = categoryRepo;
        this.scheduleRepo = scheduleRepo;
        this.budgetRepo = budgetRepo;
    }

    public AccountRecordDtos.AccountRecordResponse create(Long memberId, AccountRecordDtos.CreateRequest req) {
        Member member = memberRepo.findById(memberId).orElseThrow(() -> new SecurityException("회원이 없습니다."));
        Category category = categoryRepo.findById(req.categoryId()).orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));

        if (req.scheduleId() != null) {
            scheduleRepo.findByIdAndMember_Id(req.scheduleId(), memberId)
                    .orElseThrow(() -> new IllegalArgumentException("연결할 일정이 없습니다."));
        }
        Category sourceCategory = findSourceCategory(req.sourceCategoryId());

        AccountRecord saved = repo.save(new AccountRecord(member, category, sourceCategory,
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

    @Transactional(readOnly = true)
    public AccountRecordDtos.SummaryResponse summary(Long memberId, int year, int month) {
        List<AccountRecord> records = findByMemberAndOptionalMonth(memberId, year, month);

        long totalIncome = sumByType(records, CategoryType.INCOME);
        long totalExpense = sumByType(records, CategoryType.EXPENSE);

        Long limitAmount = budgetRepo.findByMember_IdAndYearAndMonth(memberId, year, month)
                .map(Budget::getLimitAmount).orElse(null);
        Long remainingBudget = limitAmount != null ? limitAmount - totalExpense : null;

        Double savingRate = totalIncome > 0 ? (totalIncome - totalExpense) * 100.0 / totalIncome : null;

        List<AccountRecordDtos.CategoryAmount> breakdown = categoryAmounts(records, CategoryType.EXPENSE);

        List<AccountRecordDtos.AccountRecordResponse> recent = records.stream()
                .sorted(Comparator.comparing(AccountRecord::getTransactionTime).reversed())
                .limit(RECENT_TRANSACTIONS_LIMIT)
                .map(this::toDto)
                .toList();

        return new AccountRecordDtos.SummaryResponse(
                totalIncome, totalExpense, remainingBudget, savingRate, breakdown, recent
        );
    }

    @Transactional(readOnly = true)
    public AccountRecordDtos.CategorySummaryResponse categorySummary(Long memberId, Integer year, Integer month,
                                                                      CategoryType type) {
        if ((year == null) != (month == null)) {
            throw new IllegalArgumentException("year와 month는 함께 전달해야 합니다.");
        }

        List<AccountRecord> records = (year != null)
                ? findByMemberAndOptionalMonth(memberId, year, month)
                : repo.findByMember_Id(memberId);

        List<AccountRecord> filtered = records.stream()
                .filter(r -> r.getCategory().getType() == type)
                .toList();

        long total = filtered.stream().mapToLong(r -> Math.abs(r.getAmount())).sum();

        List<AccountRecordDtos.CategorySummaryItem> items = groupAmountsByCategory(filtered).entrySet().stream()
                .map(e -> new AccountRecordDtos.CategorySummaryItem(
                        e.getKey().getId(),
                        e.getKey().getName(),
                        e.getValue(),
                        total > 0 ? e.getValue() * 100.0 / total : 0.0
                ))
                .sorted(Comparator.comparingLong(AccountRecordDtos.CategorySummaryItem::amount).reversed())
                .toList();

        return new AccountRecordDtos.CategorySummaryResponse(type.name(), total, items);
    }

    private List<AccountRecord> findByMemberAndOptionalMonth(Long memberId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay();
        return repo.findByMember_IdAndTransactionTimeBetweenOrderByTransactionTimeAsc(memberId, start, endExclusive);
    }

    private long sumByType(List<AccountRecord> records, CategoryType type) {
        return records.stream()
                .filter(r -> r.getCategory().getType() == type)
                .mapToLong(r -> Math.abs(r.getAmount()))
                .sum();
    }

    private List<AccountRecordDtos.CategoryAmount> categoryAmounts(List<AccountRecord> records, CategoryType type) {
        List<AccountRecord> filtered = records.stream()
                .filter(r -> r.getCategory().getType() == type)
                .toList();

        return groupAmountsByCategory(filtered).entrySet().stream()
                .map(e -> new AccountRecordDtos.CategoryAmount(e.getKey().getId(), e.getKey().getName(), e.getValue()))
                .sorted(Comparator.comparingLong(AccountRecordDtos.CategoryAmount::amount).reversed())
                .toList();
    }

    // 같은 트랜잭션 안에서는 동일 id의 Category 가 1차 캐시로 동일 인스턴스로 돌아오므로 엔티티 자체를 키로 묶을 수 있다.
    private Map<Category, Long> groupAmountsByCategory(List<AccountRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(AccountRecord::getCategory,
                        Collectors.summingLong(r -> Math.abs(r.getAmount()))));
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
        Category sourceCategory = findSourceCategory(req.sourceCategoryId());

        ar.update(category, sourceCategory, req.amount(), req.transactionTime(), req.scheduleId());
        return toDto(ar);
    }

    public void delete(Long memberId, Long id) {
        AccountRecord ar = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("내역이 없습니다."));
        repo.delete(ar);
    }

    // 수입원은 반드시 INCOME 카테고리여야 한다(지출 카테고리를 수입원으로 잘못 넣는 것을 막는다).
    private Category findSourceCategory(Long sourceCategoryId) {
        if (sourceCategoryId == null) return null;
        Category category = categoryRepo.findById(sourceCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("수입원이 없습니다."));
        if (category.getType() != CategoryType.INCOME) {
            throw new IllegalArgumentException("수입원 카테고리가 아닙니다.");
        }
        return category;
    }

    private AccountRecordDtos.AccountRecordResponse toDto(AccountRecord ar) {
        Category source = ar.getSourceCategory();
        return new AccountRecordDtos.AccountRecordResponse(
                ar.getId(),
                ar.getCategory().getId(),
                ar.getCategory().getName(),
                ar.getCategory().getType().name(),
                source != null ? source.getId() : null,
                source != null ? source.getName() : null,
                ar.getAmount(),
                ar.getTransactionTime(),
                ar.getScheduleId()
        );
    }
}
