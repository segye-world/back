package com.segye.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByMember_IdAndDateOrderByStartHourAsc(Long memberId, LocalDate date);

    Optional<Schedule> findByIdAndMember_Id(Long id, Long memberId);

    void deleteByMember_Id(Long memberId);
}
