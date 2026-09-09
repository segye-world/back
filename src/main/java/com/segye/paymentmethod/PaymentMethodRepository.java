package com.segye.paymentmethod;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByMember_IdOrderByIdAsc(Long memberId);

    Optional<PaymentMethod> findByIdAndMember_Id(Long id, Long memberId);

    boolean existsByMember_IdAndName(Long memberId, String name);

    void deleteByMember_Id(Long memberId);
}
