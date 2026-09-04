package com.amorim.finance_manager.creditcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados para registrar uma compra à vista no cartão")
public record CreateCreditCardPurchaseRequest(

        @NotBlank(message = "Descrição é obrigatória")
        @Size(
                max = 255,
                message = "Descrição deve possuir no máximo 255 caracteres"
        )
        @Schema(
                description = "Descrição da compra",
                example = "Compra no supermercado"
        )
        String description,

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
        @Schema(description = "Valor total da compra", example = "350.90")
        BigDecimal amount,

        @NotNull(message = "Data da compra é obrigatória")
        @Schema(
                description = "Data em que a compra foi realizada",
                example = "2026-09-04",
                format = "date"
        )
        LocalDate purchaseDate,

        @NotNull(message = "Categoria é obrigatória")
        @Schema(
                description = "Categoria de despesa da compra",
                format = "uuid"
        )
        UUID categoryId
) {
    public CreateCreditCardPurchaseRequest {
        description = description == null ? null : description.trim();
    }
}
