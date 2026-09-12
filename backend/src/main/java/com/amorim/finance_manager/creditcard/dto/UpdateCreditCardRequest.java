package com.amorim.finance_manager.creditcard.dto;

import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;
@Schema(description = """
                Campos opcionais para atualização parcial de um cartão
                de crédito. Ao menos um campo deve ser informado.
                """
)
public record UpdateCreditCardRequest(

        @Schema(
                description = "Novo nome utilizado para identificar o cartão",
                example = "Cartão viagens",
                maxLength = 120
        )
        @Size(max = 120)
        String name,

        @Schema(
                description = """
                        Novo limite total do cartão. O valor já comprometido
                        será preservado e o limite disponível será recalculado.
                        O novo limite não pode ser menor que o valor comprometido.
                        """,
                example = "6500.00",
                minimum = "0.01"
        )
        @DecimalMin(
                value = "0.01",
                message = "Limite de crédito deve ser maior que zero"
        )
        @Digits(integer = 17, fraction = 2)
        BigDecimal creditLimit,

        @Schema(
                description = "Novo dia de fechamento, entre 1 e 31",
                example = "12",
                minimum = "1",
                maximum = "31"
        )
        @Min(1)
        @Max(31)
        Integer closingDay,

        @Schema(
                description = "Novo dia de vencimento, entre 1 e 31",
                example = "19",
                minimum = "1",
                maximum = "31"
        )
        @Min(1)
        @Max(31)
        Integer dueDay,

        @Schema(
                description = """
                        Nova conta padrão para pagamento da fatura.
                        A conta precisa pertencer ao usuário autenticado.
                        """,
                example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                type = "string",
                format = "uuid"
        )
        UUID defaultAccountId,

        @Schema(
                description = """
                        Novo estado do cartão. Somente cartões ACTIVE poderão
                        receber novas compras nas próximas tasks.
                        """,
                example = "BLOCKED",
                allowableValues = {
                        "ACTIVE",
                        "INACTIVE",
                        "BLOCKED"
                }
        )
        CreditCardStatus status
) {
    public UpdateCreditCardRequest {
        name = name == null ? null : name.trim();
    }
}
