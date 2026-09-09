package com.segye.member;

import com.segye.account.AccountRecordRepository;
import com.segye.common.ApiResponse;
import com.segye.paymentmethod.PaymentMethodRepository;
import com.segye.schedule.ScheduleRepository;
import com.segye.todo.TodoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRecordRepository accountRecordRepository;
    private final TodoRepository todoRepository;
    private final ScheduleRepository scheduleRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public MemberController(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                            AccountRecordRepository accountRecordRepository,
                            TodoRepository todoRepository, ScheduleRepository scheduleRepository,
                            PaymentMethodRepository paymentMethodRepository) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRecordRepository = accountRecordRepository;
        this.todoRepository = todoRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(Authentication auth,
                                            @RequestBody @Valid ChangePasswordRequest req) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        Long memberId = (Long) auth.getPrincipal();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SecurityException("회원이 없습니다."));
        if (!passwordEncoder.matches(req.currentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        member.changePassword(passwordEncoder.encode(req.newPassword()));
        memberRepository.save(member);
        return ApiResponse.ok();
    }

    @Transactional
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        Long memberId = (Long) auth.getPrincipal();
        // 가계부 기록이 지출 수단을 참조하므로 기록을 먼저 지운 뒤 수단을 지운다.
        accountRecordRepository.deleteByMember_Id(memberId);
        paymentMethodRepository.deleteByMember_Id(memberId);
        todoRepository.deleteByMember_Id(memberId);
        scheduleRepository.deleteByMember_Id(memberId);
        memberRepository.deleteById(memberId);
        return ApiResponse.ok();
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword
    ) {}
}
