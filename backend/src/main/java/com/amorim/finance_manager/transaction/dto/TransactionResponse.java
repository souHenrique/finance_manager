package com.amorim.finance_manager.transaction.dto;

import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados de uma movimentação financeira")
public record TransactionResponse(

        @Schema(
                description = "Identificador da transação",
                example = "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
                format = "uuid"
        )
        UUID id,

        @Schema(description = "Descrição da movimentação", example = "Compra no supermercado")
        String description,

        @Schema(description = "Valor da movimentação", example = "180.50")
        BigDecimal amount,

        @Schema(description = "Data de competência financeira", example = "2026-09-02", format = "date")
        LocalDate competenceDate,

        @Schema(
                description = "Data em que a movimentação afetou o saldo",
                example = "2026-09-02",
                format = "date"
        )
        LocalDate effectiveDate,

        @Schema(description = "Data de vencimento, quando aplicável", example = "2026-09-10", format = "date")
        LocalDate dueDate,

        @Schema(description = "Tipo da movimentação", example = "EXPENSE")
        TransactionType type,

        @Schema(description = "Status da movimentação", example = "COMPLETED")
        TransactionStatus status,

        @Schema(description = "Forma de pagamento", example = "PIX")
        PaymentMethod paymentMethod,

        @Schema(description = "Conta da qual o valor foi debitado", format = "uuid")
        UUID sourceAccountId,

        @Schema(description = "Conta na qual o valor foi creditado", format = "uuid")
        UUID destinationAccountId,

        @Schema(description = "Categoria financeira da movimentação", format = "uuid")
        UUID categoryId,

        @Schema(description = "Cartão de crédito relacionado, quando aplicável", format = "uuid")
        UUID creditCardId,

        @Schema(description = "Fatura relacionada, quando aplicável", format = "uuid")
        UUID invoiceId,

        @Schema(description = "Grupo de parcelamento relacionado, quando aplicável", format = "uuid")
        UUID installmentGroupId,

        @Schema(description = "Número da parcela, quando aplicável", example = "1")
        Integer installmentNumber,

        @Schema(description = "Quantidade total de parcelas, quando aplicável", example = "12")
        Integer installmentCount,

        @Schema(
                description = "Data e hora de criação da transação",
                example = "2026-09-02T12:00:00Z",
                format = "date-time"
        )
        Instant createdAt,

        @Schema(
                description = "Data e hora da última atualização",
                example = "2026-09-02T12:30:00Z",
                format = "date-time"
        )
        Instant updatedAt

) {
}
