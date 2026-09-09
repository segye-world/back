package com.segye.paymentmethod;

import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.paymentmethod.dto.PaymentMethodDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentMethodService {

    private final PaymentMethodRepository repo;
    private final MemberRepository memberRepo;

    public PaymentMethodService(PaymentMethodRepository repo, MemberRepository memberRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodDtos.PaymentMethodResponse> list(Long memberId) {
        return repo.findByMember_IdOrderByIdAsc(memberId)
                .stream().map(this::toDto).toList();
    }

    public PaymentMethodDtos.PaymentMethodResponse create(Long memberId, PaymentMethodDtos.UpsertRequest req) {
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new SecurityException("회원이 없습니다."));

        String name = normalize(req.name());
        if (repo.existsByMember_IdAndName(memberId, name)) {
            throw new IllegalArgumentException("이미 있는 지출 수단입니다.");
        }

        PaymentMethod saved = repo.save(new PaymentMethod(member, name));
        return toDto(saved);
    }

    public PaymentMethodDtos.PaymentMethodResponse update(Long memberId, Long id, PaymentMethodDtos.UpsertRequest req) {
        PaymentMethod method = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("지출 수단이 없습니다."));

        String name = normalize(req.name());
        // 이름을 바꾸지 않는 수정(예: 같은 값 재전송)은 중복으로 보지 않는다.
        if (!method.getName().equals(name) && repo.existsByMember_IdAndName(memberId, name)) {
            throw new IllegalArgumentException("이미 있는 지출 수단입니다.");
        }

        method.update(name);
        return toDto(method);
    }

    public void delete(Long memberId, Long id) {
        PaymentMethod method = repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("지출 수단이 없습니다."));
        // 이미 기록된 가계부 내역은 지우지 않는다. account_record.payment_method_id 는
        // DB 레벨에서 NULL 로 떨어진다(AccountRecord 의 @OnDelete SET_NULL).
        repo.delete(method);
    }

    private String normalize(String name) {
        return name.trim();
    }

    private PaymentMethodDtos.PaymentMethodResponse toDto(PaymentMethod m) {
        return new PaymentMethodDtos.PaymentMethodResponse(m.getId(), m.getName());
    }
}
