package com.amorim.finance_manager.creditcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados necessários para criar um cartão de crédito")
public record CreateCreditCardRequest(

        @Schema(
                description = "Nome utilizado para identificar o cartão",
                example = "Cartão principal",
                maxLength = 120,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120)
        String name,

        @Schema(
                description = """
                        Limite total concedido ao cartão. O limite disponível
                        será inicializado automaticamente com este valor.
                        """,
                example = "5000.00",
                minimum = "0.01",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Limite de crédito é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Limite de crédito deve ser maior que zero"
        )
        @Digits(integer = 17, fraction = 2)
        BigDecimal creditLimit,

        @Schema(
                description = """
                        Dia usado como referência para o fechamento da fatura,
                        com valor entre 1 e 31.
                        """,
                example = "10",
                minimum = "1",
                maximum = "31",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Dia de fechamento é obrigatório")
        @Min(value = 1, message = "Dia de fechamento deve estar entre 1 e 31")
        @Max(value = 31, message = "Dia de fechamento deve estar entre 1 e 31")
        Integer closingDay,

        @Schema(
                description = """
                        Dia usado como referência para o vencimento da fatura,
                        com valor entre 1 e 31.
                        """,
                example = "17",
                minimum = "1",
                maximum = "31",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Dia de vencimento é obrigatório")
        @Min(value = 1, message = "Dia de vencimento deve estar entre 1 e 31")
        @Max(value = 31, message = "Dia de vencimento deve estar entre 1 e 31")
        Integer dueDay,

        @Schema(
                description = """
                        Identificador da conta padrão para pagamento da fatura.
                        A conta precisa pertencer ao usuário autenticado.
                        """,
                example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                type = "string",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Conta padrão é obrigatória")
        UUID defaultAccountId
) {
    public CreateCreditCardRequest {
        name = name == null ? null : name.trim();
    }
}
