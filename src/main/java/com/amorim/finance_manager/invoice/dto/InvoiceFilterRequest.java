package com.amorim.finance_manager.invoice.dto;

import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

@Schema(description = "Filtros opcionais para consulta de faturas")
public record InvoiceFilterRequest(

        @Schema(
                description = "Identificador do cartão",
                format = "uuid"
        )
        UUID creditCardId,

        @Min(value = 1, message = "Mês de referência deve estar entre 1 e 12")
        @Max(value = 12, message = "Mês de referência deve estar entre 1 e 12")
        @Schema(description = "Mês de referência", example = "9")
        Integer referenceMonth,

        @Min(value = 1, message = "Ano de referência deve ser positivo")
        @Max(value = 9999, message = "Ano de referência inválido")
        @Schema(description = "Ano de referência", example = "2026")
        Integer referenceYear,

        @Schema(description = "Status da fatura", example = "OPEN")
        InvoiceStatus status
) {
}
