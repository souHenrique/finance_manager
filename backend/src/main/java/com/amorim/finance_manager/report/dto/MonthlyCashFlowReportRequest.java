package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Período obrigatório do relatório de caixa mensal")
public record MonthlyCashFlowReportRequest(

    @NotNull(message = "O ano é obrigatório")
    @Min(value = 1, message = "O ano deve ser maior ou igual a 1")
    @Max(value = 9999, message = "O ano deve ser menor ou igual a 9999")
    @Schema(
            description = "Ano do relatório",
            example = "2026",
            minimum = "1",
            maximum = "9999",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Integer year,

    @NotNull(message = "O mês é obrigatório")
    @Min(value = 1, message = "O mês deve estar entre 1 e 12")
    @Max(value = 12, message = "O mês deve estar entre 1 e 12")
    @Schema(
            description = "Mês do relatório, de janeiro a dezembro",
            example = "9",
            minimum = "1",
            maximum = "12",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Integer month
) {
}
