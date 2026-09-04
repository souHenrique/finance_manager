package com.amorim.finance_manager.invoice.api;

import com.amorim.finance_manager.invoice.dto.InvoiceDetailResponse;
import com.amorim.finance_manager.invoice.dto.InvoiceFilterRequest;
import com.amorim.finance_manager.invoice.dto.InvoicePageResponse;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.shared.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface InvoiceApiDocs {

    @Operation(
            summary = "Listar faturas",
            description = """
                    Lista as faturas pertencentes ao usuário autenticado.

                    É possível filtrar por cartão de crédito, ano, mês e status.
                    Todos os filtros informados são combinados por AND.

                    A listagem é paginada e não inclui as transações completas
                    de cada fatura. Para consultar as transações, utilize o
                    endpoint de detalhamento da fatura.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Faturas encontradas",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoicePageResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Lista paginada de faturas",
                                    value = INVOICE_PAGE_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtros ou parâmetros de paginação inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Erro de validação",
                                            value = VALIDATION_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Parâmetro inválido",
                                            value = INVALID_REQUEST_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<InvoicePageResponse> list(
            @ParameterObject
            @Valid
            InvoiceFilterRequest filters,

            @ParameterObject
            Pageable pageable
    );

    @Operation(
            summary = "Consultar fatura por ID",
            description = """
                    Retorna os dados completos de uma fatura pertencente
                    ao usuário autenticado.

                    O detalhamento inclui as transações vinculadas à fatura,
                    quando aplicável.

                    Caso a fatura não exista ou pertença a outro usuário,
                    a API retorna HTTP 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Fatura encontrada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoiceDetailResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Detalhamento de fatura",
                                    value = INVOICE_DETAIL_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID da fatura inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "UUID inválido",
                                    value = INVALID_REQUEST_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Fatura não encontrada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Fatura não encontrada",
                                    value = INVOICE_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<InvoiceDetailResponse> findById(
            @Parameter(
                    name = "id",
                    description = "Identificador UUID da fatura",
                    required = true,
                    example = "72486234-ef50-4c7e-99a7-9193a28533a8",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id
    );

    @Operation(
            summary = "Listar faturas de um cartão",
            description = """
                    Lista as faturas de um cartão de crédito pertencente
                    ao usuário autenticado.

                    A consulta pode ser filtrada por ano, mês e status.
                    Os resultados são paginados.

                    Caso o cartão não exista ou pertença a outro usuário,
                    a API retorna HTTP 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Faturas do cartão encontradas",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoicePageResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Faturas do cartão",
                                    value = INVOICE_PAGE_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtros, UUID ou paginação inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Erro de validação",
                                            value = VALIDATION_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Parâmetro inválido",
                                            value = INVALID_REQUEST_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cartão de crédito não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Cartão não encontrado",
                                    value = CREDIT_CARD_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<InvoicePageResponse> listByCreditCard(
            @Parameter(
                    name = "id",
                    description = "Identificador UUID do cartão de crédito",
                    required = true,
                    example = "0c736743-8885-43d1-813b-c096a4899201",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id,

            @Parameter(
                    name = "referenceYear",
                    description = "Ano de referência da fatura",
                    example = "2026",
                    schema = @Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "9999"
                    )
            )
            @Min(value = 1, message = "Ano de referência deve ser positivo")
            @Max(value = 9999, message = "Ano de referência inválido")
            Integer referenceYear,

            @Parameter(
                    name = "referenceMonth",
                    description = "Mês de referência da fatura, de 1 a 12",
                    example = "9",
                    schema = @Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "12"
                    )
            )
            @Min(value = 1, message = "Mês de referência deve estar entre 1 e 12")
            @Max(value = 12, message = "Mês de referência deve estar entre 1 e 12")
            Integer referenceMonth,

            @Parameter(
                    name = "status",
                    description = "Status atual da fatura",
                    example = "OPEN",
                    schema = @Schema(
                            implementation = InvoiceStatus.class
                    )
            )
            InvoiceStatus status,

            @ParameterObject
            Pageable pageable
    );
}
