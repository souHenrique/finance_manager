package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Relatório de caixa mensal do usuário autenticado")
public record MonthlyCashFlowResponse(

        @Schema(description = "Ano consultado", example = "2026")
        int year,

        @Schema(description = "Mês consultado, de 1 a 12", example = "9")
        int month,

        @Schema(
                description = "Primeiro dia do mês, inclusive",
                format = "date",
                example = "2026-09-01"
        )
        LocalDate startDate,

        @Schema(
                description = "Último dia do mês, inclusive",
                format = "date",
                example = "2026-09-30"
        )
        LocalDate endDate,

        @Schema(description = "Totais de caixa e agrupamentos por categoria")
        CashFlowSummaryResponse summary
) {
}
