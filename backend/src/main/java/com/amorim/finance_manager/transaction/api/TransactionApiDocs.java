package com.amorim.finance_manager.transaction.api;

import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.transaction.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface TransactionApiDocs {

    @Operation(
            summary = "Criar transação",
            description = "Cria uma receita ou despesa, pendente ou concluída",
            requestBody =
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreateTransactionRequest.class
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Despesa",
                                            value = CREATE_EXPENSE_TRANSACTION_REQUEST
                                    ),
                                    @ExampleObject(
                                            name = "Receita",
                                            value = CREATE_INCOME_TRANSACTION_REQUEST
                                    ),
                                    @ExampleObject(
                                            name = "Pendente",
                                            value = CREATE_PENDING_TRANSACTION_REQUEST
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transação criada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TransactionResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = TRANSACTION_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados ou regra da transação inválidos",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = VALIDATION_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conta ou categoria não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conta inativa ou conflito concorrente",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = OPTIMISTIC_LOCK_CONFLICT
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<TransactionResponse> create(
            CreateTransactionRequest request
    );

    @Operation(
            summary = "Consultar transação",
            description = "Retorna uma movimentação pertencente ao usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transação encontrada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TransactionResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = TRANSACTION_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transação não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = TRANSACTION_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<TransactionResponse> findById(
            @Parameter(
                    name = "id",
                    description = "Identificador da transação",
                    required = true,
                    example = "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id
    );

    @Operation(
            summary = "Consultar parcelas de uma compra",
            description = "Retorna o valor integral e as parcelas de uma compra no cartão pertencente ao usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Parcelas encontradas",
                    content = @Content(
                            schema = @Schema(implementation = TransactionInstallmentDetailsResponse.class),
                            examples = @ExampleObject(value = TRANSACTION_INSTALLMENT_DETAILS_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transação não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    ResponseEntity<TransactionInstallmentDetailsResponse> findInstallmentDetails(
            @Parameter(
                    name = "id",
                    description = "Identificador da transação de referência",
                    required = true,
                    example = "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id
    );

    @Operation(
            summary = "Atualizar transação",
            description = "Reverte o impacto antigo e aplica o novo impacto",
            requestBody =
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = UpdateTransactionRequest.class
                            ),
                            examples = @ExampleObject(
                                    value = UPDATE_TRANSACTION_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transação atualizada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TransactionResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = UPDATED_TRANSACTION_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Atualização inválida",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transação, conta ou categoria não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Status inválido ou conflito concorrente",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Status inválido",
                                            value = INVALID_TRANSACTION_STATUS
                                    ),
                                    @ExampleObject(
                                            name = "Conflito concorrente",
                                            value = OPTIMISTIC_LOCK_CONFLICT
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<TransactionResponse> update(
            @Parameter(
                    name = "id",
                    description = "Identificador da transação",
                    required = true,
                    example = "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id,
            UpdateTransactionRequest request
    );

    @Operation(
            summary = "Cancelar transação",
            description = "Cancela sem excluir e reverte o impacto financeiro"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transação cancelada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TransactionResponse.class
                            ),
                            examples = @ExampleObject(value = CANCELLED_TRANSACTION_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transação não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = TRANSACTION_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Transação já cancelada ou conflito concorrente",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Cancelamento duplicado",
                                            value = TRANSACTION_ALREADY_CANCELLED
                                    ),
                                    @ExampleObject(
                                            name = "Conflito concorrente",
                                            value = OPTIMISTIC_LOCK_CONFLICT
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<TransactionResponse> cancel(
            @Parameter(
                    name = "id",
                    description = "Identificador da transação",
                    required = true,
                    example = "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id
    );

    @Operation(
            summary = "Listar transações",
            description = """
                Lista somente transações do usuário autenticado.
                Os filtros são opcionais e combinados com AND.
                O período considera competenceDate, com limites inclusivos.
                accountId considera origem ou destino.
                A descrição usa busca parcial sem diferenciar maiúsculas e minúsculas.
                Sem filtro de status, inclui todos os status.
                A paginação começa em zero, com tamanho padrão 20 e máximo efetivo 100.
                Compras parceladas aparecem uma única vez, com o valor integral em displayAmount.
                As parcelas individuais estão disponíveis no detalhe da compra.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de transações, podendo estar vazia",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = TransactionPageResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Página vazia",
                                    value = TRANSACTION_PAGE_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtros ou ordenação inválidos",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<TransactionPageResponse> list(
            @ParameterObject TransactionFilterRequest filters,
            @ParameterObject Pageable pageable
    );
}
