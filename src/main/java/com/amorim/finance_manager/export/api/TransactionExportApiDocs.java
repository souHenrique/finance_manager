package com.amorim.finance_manager.export.api;

import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.transaction.dto.TransactionFilterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.TRANSACTION_EXPORT_CSV;

public interface TransactionExportApiDocs {

    @Operation(
            summary = "Exportar transações em CSV",
            description = """
                    Exporta as transações do usuário autenticado.

                    Aceita os mesmos filtros da consulta de transações.
                    startDate e endDate filtram por competenceDate.

                    O arquivo utiliza UTF-8, datas ISO-8601, valores
                    monetários sem formatação regional e cabeçalho estável.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Arquivo CSV, podendo conter apenas o cabeçalho",
                    content = @Content(
                            mediaType = "text/csv",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Exportação CSV",
                                    value = TRANSACTION_EXPORT_CSV
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtros inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<byte[]> exportTransactions(@Valid @ParameterObject TransactionFilterRequest filters);
}
