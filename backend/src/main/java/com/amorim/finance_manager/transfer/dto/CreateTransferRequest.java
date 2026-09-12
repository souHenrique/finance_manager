package com.amorim.finance_manager.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados para transferência entre duas contas do mesmo usuário")
public record CreateTransferRequest(

        @Schema(
                description = "Conta que será debitada",
                example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                format = "uuid"
        )
        @NotNull(message = "Conta de origem é obrigatória")
        UUID sourceAccountId,

        @Schema(
                description = "Conta que será creditada",
                example = "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
                format = "uuid"
        )
        @NotNull(message = "Conta de destino é obrigatória")
        UUID destinationAccountId,

        @Schema(description = "Valor positivo da transferência", example = "250.00")
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Valor deve possuir no máximo 17 dígitos inteiros e 2 decimais"
        )
        BigDecimal amount,

        @Schema(description = "Data da transferência", example = "2026-09-02", format = "date")
        @NotNull(message = "Data é obrigatória")
        LocalDate date,

        @Schema(description = "Descrição da transferência", example = "Transferência para reserva")
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255, message = "Descrição deve possuir no máximo 255 caracteres")
        String description
) {
    public CreateTransferRequest {
        if (description != null) {
            description = description.trim();
        }
    }
}
