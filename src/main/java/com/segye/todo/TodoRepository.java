package com.segye.todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByMember_IdAndDateOrderByIdAsc(Long memberId, LocalDate date);

    Optional<Todo> findByIdAndMember_Id(Long id, Long memberId);

    void deleteByMember_Id(Long memberId);
}
