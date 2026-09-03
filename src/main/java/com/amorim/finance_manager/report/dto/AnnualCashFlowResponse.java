package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Evolução anual do fluxo de caixa")
public record AnnualCashFlowResponse(

        @Schema(description = "Ano consultado", example = "2026")
        int year,

        @Schema(description = "Primeiro dia do ano", example = "2026-01-01")
        LocalDate startDate,

        @Schema(description = "Último dia do ano", example = "2026-12-31")
        LocalDate endDate,

        @Schema(description = "Evolução contendo obrigatoriamente os 12 meses")
        List<AnnualCashFlowMonthResponse> evolution
) {
}
