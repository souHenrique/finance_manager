package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Intervalo de datas e respectivo resumo de caixa")
public record CashFlowPeriodResponse(

        @Schema(
                description = "Data inicial do período, inclusive",
                format = "date",
                example = "2026-08-31"
        )
        LocalDate startDate,

        @Schema(
                description = "Data final do período, inclusive",
                format = "date",
                example = "2026-09-06"
        )
        LocalDate endDate,

        @Schema(description = "Resumo de caixa das movimentações efetivadas no intervalo")
        CashFlowSummaryResponse summary
) {
}
