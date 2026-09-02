package com.amorim.finance_manager.transaction.dto;

import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados opcionais para atualização de uma transação")
public record UpdateTransactionRequest(

        @Schema(description = "Nova descrição da movimentação", example = "Compra mensal no supermercado")
        @Size(min = 1, max = 255)
        String description,

        @Schema(description = "Novo valor positivo da movimentação", example = "210.90")
        @DecimalMin("0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @Schema(description = "Nova data de competência", example = "2026-09-02", format = "date")
        LocalDate competenceDate,

        @Schema(
                description = "Nova data em que a movimentação afeta o saldo",
                example = "2026-09-02",
                format = "date"
        )
        LocalDate effectiveDate,

        @Schema(description = "Novo status da movimentação", example = "COMPLETED")
        TransactionStatus status,

        @Schema(description = "Nova forma de pagamento", example = "DEBIT")
        PaymentMethod paymentMethod,

        @Schema(
                description = "Nova conta da qual o valor é debitado",
                example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                format = "uuid"
        )
        UUID sourceAccountId,

        @Schema(
                description = "Nova conta na qual o valor é creditado",
                example = "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
                format = "uuid"
        )
        UUID destinationAccountId,

        @Schema(
                description = "Nova categoria financeira da movimentação",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709",
                format = "uuid"
        )
        UUID categoryId
) {
    public UpdateTransactionRequest {
        if (description != null) {
            description = description.trim();
        }
    }
}
