package com.amorim.finance_manager.creditcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreditCardRefundRequest(

        @NotBlank(message = "O motivo do estorno é obrigatório")
        @Size(
                max = 500,
                message = "O motivo deve ter no máximo 500 caracteres"
        )
        String reason
) {
}
