package com.segye.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRecordRepository extends JpaRepository<AccountRecord, Long> {

    List<AccountRecord> findByMember_IdAndTransactionTimeBetweenOrderByTransactionTimeAsc(
            Long memberId, LocalDateTime from, LocalDateTime to
    );

    List<AccountRecord> findByMember_IdAndTransactionTimeBetweenOrderByTransactionTimeDesc(
            Long memberId, LocalDateTime from, LocalDateTime to
    );

    Optional<AccountRecord> findByIdAndMember_Id(Long id, Long memberId);

    void deleteByMember_Id(Long memberId);
}
