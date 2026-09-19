package com.amorim.finance_manager.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Detalhes de uma compra parcelada no cartão")
public record TransactionInstallmentDetailsResponse(

        @Schema(description = "Valor integral da compra", example = "1200.00")
        BigDecimal totalAmount,

        @Schema(description = "Parcelas que compõem a compra, em ordem crescente")
        List<TransactionResponse> installments
) {
}
