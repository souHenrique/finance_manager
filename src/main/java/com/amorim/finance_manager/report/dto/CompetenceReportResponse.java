package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Relatório financeiro calculado pelo regime de competência")
public record CompetenceReportResponse(

        @Schema(description = "Data inicial do período", example = "2026-09-01")
        LocalDate startDate,

        @Schema(description = "Data final do período", example = "2026-09-30")
        LocalDate endDate,

        @Schema(description = "Total de receitas por competência", example = "5000.00")
        BigDecimal totalIncome,

        @Schema(description = "Total de despesas por competência", example = "1800.00")
        BigDecimal totalExpenses,

        @Schema(description = "Resultado do período: totalIncome menos totalExpenses", example = "3200.00")
        BigDecimal result,

        @Schema(description = "Receitas agrupadas por categoria")
        List<CategoryCashFlowResponse> incomeCategories,

        @Schema(description = "Despesas agrupadas por categoria")
        List<CategoryCashFlowResponse> expenseCategories
) {
}
