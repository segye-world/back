package com.segye.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByMember_IdAndYearAndMonth(Long memberId, int year, int month);

    void deleteByMember_Id(Long memberId);
}
