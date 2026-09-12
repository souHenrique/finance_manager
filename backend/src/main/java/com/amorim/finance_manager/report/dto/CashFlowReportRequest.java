package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "Parâmetros para consulta de relatórios de caixa")
public record CashFlowReportRequest(

        @NotNull(message = "A data é obrigatória")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(
                description = """
                        Data de referência no formato yyyy-MM-dd.
                        No relatório diário, corresponde ao dia consultado.
                        No semanal, pode ser qualquer dia da semana desejada.
                        Todo o período calculado, incluindo a semana anterior,
                        deve estar entre 0001-01-01 e 9999-12-31.
                        """,
                type = "string",
                format = "date",
                example = "2026-09-03",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate date
) {
}
