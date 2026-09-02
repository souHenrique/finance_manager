package com.amorim.finance_manager.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erro de validação associado a um campo da requisição")
public record FieldErrorResponse(

        @Schema(description = "Nome do campo inválido", example = "name")
        String field,

        @Schema(description = "Motivo pelo qual o valor foi rejeitado", example = "Nome é obrigatório")
        String message
) {
}
