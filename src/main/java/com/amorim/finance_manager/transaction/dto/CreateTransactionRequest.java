package com.amorim.finance_manager.transaction.dto;

import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados para criação de uma receita ou despesa")
public record CreateTransactionRequest(

        @Schema(description = "Descrição da movimentação", example = "Compra no supermercado")
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255, message = "Descrição deve possuir no máximo 255 caracteres")
        String description,

        @Schema(description = "Valor positivo da movimentação", example = "180.50")
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Valor deve ser maior que zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Valor deve possuir no máximo 17 dígitos inteiros e 2 decimais"
        )
        BigDecimal amount,

        @Schema(description = "Data de competência financeira", example = "2026-09-02", format = "date")
        @NotNull(message = "Data de competência é obrigatória")
        LocalDate competenceDate,

        @Schema(
                description = "Data em que a movimentação afeta o saldo; ausente quando pendente",
                example = "2026-09-02",
                format = "date"
        )
        LocalDate effectiveDate,

        @Schema(description = "Data de vencimento, quando aplicável", example = "2026-09-10", format = "date")
        LocalDate dueDate,

        @Schema(description = "Tipo da movimentação", example = "EXPENSE")
        @NotNull(message = "Tipo é obrigatório")
        TransactionType type,

        @Schema(description = "Status da movimentação", example = "COMPLETED")
        @NotNull(message = "Status é obrigatório")
        TransactionStatus status,

        @Schema(description = "Forma de pagamento", example = "PIX")
        @NotNull(message = "Método de pagamento é obrigatório")
        PaymentMethod paymentMethod,

        @Schema(
                description = "Conta da qual o valor é debitado em uma despesa",
                example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                format = "uuid"
        )
        UUID sourceAccountId,

        @Schema(
                description = "Conta na qual o valor é creditado em uma receita",
                example = "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
                format = "uuid"
        )
        UUID destinationAccountId,

        @Schema(
                description = "Categoria financeira da movimentação",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709",
                format = "uuid"
        )
        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        @Schema(description = "Cartão de crédito relacionado, quando aplicável", format = "uuid")
        UUID creditCardId,

        @Schema(description = "Fatura relacionada, quando aplicável", format = "uuid")
        UUID invoiceId,

        @Schema(description = "Grupo de parcelamento relacionado, quando aplicável", format = "uuid")
        UUID installmentGroupId,

        @Schema(description = "Número da parcela, quando aplicável", example = "1")
        @Positive(message = "Número da parcela deve ser maior que zero")
        Integer installmentNumber,

        @Schema(description = "Quantidade total de parcelas, quando aplicável", example = "12")
        @Positive(message = "Quantidade de parcelas deve ser maior que zero")
        Integer installmentCount

) {
    public CreateTransactionRequest {
        if (description != null) {
            description = description.trim();
        }
    }
}
