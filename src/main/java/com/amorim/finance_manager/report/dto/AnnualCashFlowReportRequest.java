package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Período obrigatório do relatório de caixa anual")
public record AnnualCashFlowReportRequest(

        @NotNull(message = "O ano é obrigatório")
        @Min(value = 1, message = "O ano deve ser maior ou igual a 1")
        @Max(value = 9999, message = "O ano deve ser menor ou igual a 9999")
        @Schema(
                description = "Ano civil consultado, de janeiro a dezembro",
                example = "2026",
                minimum = "1",
                maximum = "9999",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Integer year
) {
}
