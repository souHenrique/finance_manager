package com.amorim.finance_manager.invoice.dto;

import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detalhamento de uma fatura e de suas transações")
public record InvoiceDetailResponse(

        @Schema(
                description = "Identificador da fatura",
                example = "72486234-ef50-4c7e-99a7-9193a28533a8",
                format = "uuid"
        )
        UUID id,

        @Schema(
                description = "Identificador do cartão de crédito da fatura",
                example = "0c736743-8885-43d1-813b-c096a4899201",
                format = "uuid"
        )
        UUID creditCardId,

        @Schema(description = "Mês de referência da fatura, de 1 a 12", example = "9")
        Integer referenceMonth,

        @Schema(description = "Ano de referência da fatura", example = "2026")
        Integer referenceYear,

        @Schema(
                description = "Data de fechamento da fatura",
                example = "2026-09-20",
                format = "date"
        )
        LocalDate closingDate,

        @Schema(
                description = "Data de vencimento da fatura",
                example = "2026-09-28",
                format = "date"
        )
        LocalDate dueDate,

        @Schema(description = "Valor total acumulado na fatura", example = "850.75")
        BigDecimal totalAmount,

        @Schema(description = "Status atual da fatura", example = "OPEN")
        InvoiceStatus status,

        @Schema(
                description = "Data e hora do pagamento; nula enquanto a fatura não estiver paga",
                example = "2026-09-28T14:30:00Z",
                format = "date-time"
        )
        Instant paidAt,

        @Schema(
                description = "Versão utilizada no controle de concorrência otimista",
                example = "0"
        )
        Long version,

        @Schema(
                description = "Transações vinculadas à fatura, ordenadas pela data de competência"
        )
        List<TransactionResponse> transactions
) {
}
