package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "Período do relatório por competência")
public record CompetenceReportRequest(

        @NotNull(message = "A data inicial é obrigatória")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(
                description = "Primeiro dia do período, inclusive",
                example = "2026-09-01",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate startDate,

        @NotNull(message = "A data final é obrigatória")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(
                description = "Último dia do período, inclusive",
                example = "2026-09-30",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate endDate
) {
}
