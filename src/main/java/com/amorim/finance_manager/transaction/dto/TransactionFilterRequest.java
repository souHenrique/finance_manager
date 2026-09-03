package com.amorim.finance_manager.transaction.dto;

import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Filtros opcionais para consulta de transações")
public record TransactionFilterRequest(

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(
                description = "Data inicial de competência, inclusive",
                example = "2026-09-01",
                format = "date"
        )
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(
                description = "Data final de competência, inclusive",
                example = "2026-09-30",
                format = "date"
        )
        LocalDate endDate,

        @Schema(
                description = "Identificador exato da categoria",
                format = "uuid"
        )
        UUID categoryId,

        @Schema(
                description = "Conta de origem ou de destino",
                format = "uuid"
        )
        UUID accountId,

        @Schema(
                description = "Identificador do cartão de crédito",
                format = "uuid"
        )
        UUID creditCardId,

        @Schema(description = "Tipo da transação", example = "EXPENSE")
        TransactionType type,

        @Schema(description = "Status da transação", example = "COMPLETED")
        TransactionStatus status,

        @DecimalMin(value = "0.00", message = "O valor mínimo não pode ser negativo")
        @Digits(integer = 17, fraction = 2)
        @Schema(description = "Valor mínimo, inclusive", example = "50.00")
        BigDecimal minAmount,

        @DecimalMin(value = "0.00", message = "O valor máximo não pode ser negativo")
        @Digits(integer = 17, fraction = 2)
        @Schema(description = "Valor máximo, inclusive", example = "500.00")
        BigDecimal maxAmount,

        @Size(max = 255)
        @Schema(
                description = "Trecho literal da descrição, ignorando maiúsculas e minúsculas",
                example = "mercado"
        )
        String description
) {
}
