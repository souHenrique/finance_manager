package com.amorim.finance_manager.dashboard.dto;

import com.amorim.finance_manager.dashboard.model.AccountingBasis;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Indicador financeiro com seu regime de cálculo")
public record DashboardIndicatorResponse(

        @Schema(description = "Regime utilizado", example = "CASH")
        AccountingBasis basis,

        @Schema(description = "Valor monetário do indicador", example = "5000.00")
        BigDecimal amount
) {
}
