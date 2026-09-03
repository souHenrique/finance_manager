package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Valor agregado de uma categoria no período consultado")
public record CategoryCashFlowResponse(

        @Schema(
                description = """
                        Identificador da categoria.
                        Pode ser nulo para movimentações sem categoria.
                        """,
                types = {"string", "null"},
                format = "uuid",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709"
        )
        UUID categoryId,

        @Schema(
                description = """
                        Nome atual da categoria.
                        Retorna 'Sem categoria' quando não existe associação
                        ou 'Categoria indisponível' quando o nome não é encontrado.
                        """,
                example = "Supermercado"
        )
        String name,

        @Schema(
                description = "Soma dos valores das movimentações elegíveis dessa categoria",
                example = "300.00"
        )
        BigDecimal amount
) {
}
