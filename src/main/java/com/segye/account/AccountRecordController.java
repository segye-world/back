package com.segye.account;

import com.segye.account.dto.AccountRecordDtos;
import com.segye.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/account-records")
public class AccountRecordController {

    private final AccountRecordService service;

    public AccountRecordController(AccountRecordService service) {
        this.service = service;
    }

    private Long currentMemberId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public ApiResponse<AccountRecordDtos.AccountRecordResponse> create(
            Authentication auth,
            @RequestBody @Valid AccountRecordDtos.CreateRequest req
    ) {
        return ApiResponse.ok(service.create(currentMemberId(auth), req));
    }

    @GetMapping
    public ApiResponse<List<AccountRecordDtos.AccountRecordResponse>> listRange(
            Authentication auth,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return ApiResponse.ok(service.listRange(currentMemberId(auth), from, to));
    }

    @GetMapping("/monthly")
    public ApiResponse<List<AccountRecordDtos.AccountRecordResponse>> listMonthly(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.ok(service.listMonthly(currentMemberId(auth), year, month));
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountRecordDtos.AccountRecordResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody @Valid AccountRecordDtos.UpdateRequest req
    ) {
        return ApiResponse.ok(service.update(currentMemberId(auth), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        service.delete(currentMemberId(auth), id);
        return ApiResponse.ok();
    }
}
