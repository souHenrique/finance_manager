package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Totais de caixa de um mês da evolução anual")
public record AnnualCashFlowMonthResponse(

        @Schema(description = "Mês de 1 a 12", example = "9")
        int month,

        @Schema(description = "Entradas, saídas e resultado do mês")
        CashFlowTotalsResponse totals
) {
}
