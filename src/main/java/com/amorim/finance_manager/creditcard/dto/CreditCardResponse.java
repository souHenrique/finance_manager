package com.amorim.finance_manager.creditcard.dto;

import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados de um cartão de crédito")
public record CreditCardResponse(

        @Schema(
                description = "Identificador do cartão",
                example = "d89835ee-3463-4a35-a2e9-38d96ab17418",
                format = "uuid"
        )
        UUID id,

        @Schema(
                description = "Nome utilizado para identificar o cartão",
                example = "Cartão principal"
        )
        String name,

        @Schema(
                description = "Limite total de crédito",
                example = "5000.00"
        )
        BigDecimal creditLimit,

        @Schema(
                description = "Limite disponível para novas compras",
                example = "4200.00"
        )
        BigDecimal availableLimit,

        @Schema(
                description = "Dia de fechamento da fatura, entre 1 e 31",
                example = "10",
                minimum = "1",
                maximum = "31"
        )
        Integer closingDay,

        @Schema(
                description = "Dia de vencimento da fatura, entre 1 e 31",
                example = "17",
                minimum = "1",
                maximum = "31"
        )
        Integer dueDay,

        @Schema(
                description = "Identificador da conta padrão para pagamento da fatura",
                example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                format = "uuid"
        )
        UUID defaultAccountId,

        @Schema(
                description = "Estado atual do cartão",
                example = "ACTIVE"
        )
        CreditCardStatus status,

        @Schema(
                description = "Versão usada no controle de concorrência otimista",
                example = "0"
        )
        Long version
) {
}
