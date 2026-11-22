package com.segye.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByMember_IdOrderById(Long memberId);

    List<Category> findByMember_IdAndTypeOrderById(Long memberId, CategoryType type);

    Optional<Category> findByIdAndMember_Id(Long id, Long memberId);

    void deleteByMember_Id(Long memberId);
}
