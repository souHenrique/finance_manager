package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
        description = """
                Diferenças monetárias entre a semana consultada e a semana anterior.
                Todos os campos representam semana atual menos semana anterior.
                Os valores não são percentuais.
                """
)
public record CashFlowComparisonResponse(

        @Schema(
                description = "Entradas da semana atual menos entradas da semana anterior",
                example = "500.00"
        )
        BigDecimal inflowsDifference,

        @Schema(
                description = """
                        Saídas da semana atual menos saídas da semana anterior.
                        Valor positivo significa aumento das saídas.
                        """,
                example = "200.00"
        )
        BigDecimal outflowsDifference,

        @Schema(
                description = "Resultado líquido da semana atual menos resultado líquido da anterior",
                example = "300.00"
        )
        BigDecimal netDifference
) {
}
