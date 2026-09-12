package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Relatório de caixa diário do usuário autenticado")
public record DailyCashFlowResponse(

        @Schema(
                description = "Dia consultado, correspondente à effectiveDate das movimentações",
                format = "date",
                example = "2026-09-03"
        )
        LocalDate date,

        @Schema(description = "Entradas, saídas, resultado líquido e categorias do dia")
        CashFlowSummaryResponse summary
) {
}
