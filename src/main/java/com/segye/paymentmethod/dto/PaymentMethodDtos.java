package com.segye.paymentmethod.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentMethodDtos {

    public record UpsertRequest(
            @NotBlank @Size(max = 50) String name
    ) {
    }

    public record PaymentMethodResponse(
            Long id,
            String name
    ) {
    }
}
