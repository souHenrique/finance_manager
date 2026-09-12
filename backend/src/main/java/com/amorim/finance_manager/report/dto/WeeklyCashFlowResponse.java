package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Relatório de caixa semanal com comparação à semana anterior")
public record WeeklyCashFlowResponse(

        @Schema(
                description = """
                        Semana que contém a data informada,
                        de segunda-feira a domingo, inclusive.
                        Não significa necessariamente a semana de hoje.
                        """
        )
        CashFlowPeriodResponse currentWeek,

        @Schema(
                description = """
                        Semana imediatamente anterior à consultada,
                        de segunda-feira a domingo, inclusive.
                        """
        )
        CashFlowPeriodResponse previousWeek,

        @Schema(description = "Diferenças monetárias entre os resultados das duas semanas")
        CashFlowComparisonResponse comparison
) {
}
