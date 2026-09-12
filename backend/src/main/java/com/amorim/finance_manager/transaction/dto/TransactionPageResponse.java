package com.amorim.finance_manager.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Página de transações do usuário autenticado")
public record TransactionPageResponse(

    @Schema(description = "Transações presentes nesta página")
    List<TransactionResponse> content,

    @Schema(description = "Número da página, começando em zero", example = "0")
    int page,

    @Schema(description = "Tamanho configurado para a página", example = "20")
    int size,

    @Schema(description = "Total de transações que atendem aos filtros", example = "45")
    long totalElements,

    @Schema(description = "Total de páginas", example = "3")
    int totalPages,

    @Schema(description = "Indica se esta é a primeira página", example = "true")
    boolean first,

    @Schema(description = "Indica se esta é a última página", example = "false")
    boolean last
) {

        public static TransactionPageResponse from(
                Page<TransactionResponse> result
        ) {
            return new TransactionPageResponse(
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
