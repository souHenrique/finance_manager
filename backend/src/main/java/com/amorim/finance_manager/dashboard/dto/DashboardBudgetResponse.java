package com.amorim.finance_manager.dashboard.dto;

import com.amorim.finance_manager.dashboard.model.AccountingBasis;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Resumo dos orçamentos do mês atual")
public record DashboardBudgetResponse(

        @Schema(description = "Regime usado no consumo dos orçamentos", example = "COMPETENCE")
        AccountingBasis basis,

        @Schema(description = "Soma dos limites dos orçamentos do mês", example = "3000.00")
        BigDecimal totalLimit,

        @Schema(description = "Soma das despesas consumidas pelos orçamentos", example = "2100.00")
        BigDecimal totalSpent,

        @Schema(description = "Percentual agregado consumido dos orçamentos", example = "70.00")
        BigDecimal usagePercentage,

        @Schema(description = "Orçamentos do mês detalhados por categoria")
        List<DashboardBudgetItemResponse> items
) {
}
