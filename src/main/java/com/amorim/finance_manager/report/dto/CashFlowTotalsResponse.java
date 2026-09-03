package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totais monetários de caixa de um período")
public record CashFlowTotalsResponse(

        @Schema(description = "Receitas efetivas do período", example = "5000.00")
        BigDecimal inflows,

        


) {
}
