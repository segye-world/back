package com.segye.budget;

import com.segye.budget.dto.BudgetDtos;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository repo;
    private final MemberRepository memberRepo;

    public BudgetService(BudgetRepository repo, MemberRepository memberRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
    }

    public BudgetDtos.BudgetResponse upsert(Long memberId, BudgetDtos.UpsertRequest req) {
        Budget budget = repo.findByMember_IdAndYearAndMonth(memberId, req.year(), req.month()).orElse(null);

        if (budget != null) {
            budget.update(req.limitAmount());
        } else {
            Member member = memberRepo.findById(memberId)
                    .orElseThrow(() -> new SecurityException("회원이 없습니다."));
            budget = repo.save(new Budget(member, req.year(), req.month(), req.limitAmount()));
        }
        return toDto(budget);
    }

    @Transactional(readOnly = true)
    public BudgetDtos.BudgetResponse get(Long memberId, int year, int month) {
        return repo.findByMember_IdAndYearAndMonth(memberId, year, month)
                .map(this::toDto)
                .orElse(null);
    }

    private BudgetDtos.BudgetResponse toDto(Budget b) {
        return new BudgetDtos.BudgetResponse(b.getId(), b.getYear(), b.getMonth(), b.getLimitAmount());
    }
}
