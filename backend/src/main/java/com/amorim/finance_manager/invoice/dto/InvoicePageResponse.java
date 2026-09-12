package com.amorim.finance_manager.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Página de faturas")
public record InvoicePageResponse(

        @Schema(description = "Resumos das faturas presentes nesta página")
        List<InvoiceSummaryResponse> content,

        @Schema(description = "Número da página, começando em zero", example = "0")
        int page,

        @Schema(description = "Tamanho configurado para a página", example = "20")
        int size,

        @Schema(description = "Total de faturas que atendem aos filtros", example = "45")
        long totalElements,

        @Schema(description = "Total de páginas", example = "3")
        int totalPages,

        @Schema(description = "Indica se esta é a primeira página", example = "true")
        boolean first,

        @Schema(description = "Indica se esta é a última página", example = "false")
        boolean last
) {
    public static InvoicePageResponse from(Page<InvoiceSummaryResponse> result) {
        return new InvoicePageResponse(
                List.copyOf(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }
}
