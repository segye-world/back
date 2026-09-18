package com.segye.budget;

import com.segye.budget.dto.BudgetDtos;
import com.segye.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    private Long currentMemberId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public ApiResponse<BudgetDtos.BudgetResponse> create(
            Authentication auth,
            @RequestBody @Valid BudgetDtos.UpsertRequest req
    ) {
        return ApiResponse.ok(service.upsert(currentMemberId(auth), req));
    }

    @PutMapping
    public ApiResponse<BudgetDtos.BudgetResponse> update(
            Authentication auth,
            @RequestBody @Valid BudgetDtos.UpsertRequest req
    ) {
        return ApiResponse.ok(service.upsert(currentMemberId(auth), req));
    }

    @GetMapping
    public ApiResponse<BudgetDtos.BudgetResponse> get(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.ok(service.get(currentMemberId(auth), year, month));
    }
}
