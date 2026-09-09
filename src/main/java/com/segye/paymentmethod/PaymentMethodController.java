package com.segye.paymentmethod;

import com.segye.common.ApiResponse;
import com.segye.paymentmethod.dto.PaymentMethodDtos;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService service;

    public PaymentMethodController(PaymentMethodService service) {
        this.service = service;
    }

    private Long memberId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public ApiResponse<List<PaymentMethodDtos.PaymentMethodResponse>> list(Authentication auth) {
        return ApiResponse.ok(service.list(memberId(auth)));
    }

    @PostMapping
    public ApiResponse<PaymentMethodDtos.PaymentMethodResponse> create(
            Authentication auth,
            @RequestBody @Valid PaymentMethodDtos.UpsertRequest req
    ) {
        return ApiResponse.ok(service.create(memberId(auth), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<PaymentMethodDtos.PaymentMethodResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody @Valid PaymentMethodDtos.UpsertRequest req
    ) {
        return ApiResponse.ok(service.update(memberId(auth), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        service.delete(memberId(auth), id);
        return ApiResponse.ok();
    }
}
