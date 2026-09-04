package com.amorim.finance_manager.creditcard.api;

import com.amorim.finance_manager.creditcard.dto.CreateCreditCardPurchaseRequest;
import com.amorim.finance_manager.creditcard.dto.CreateCreditCardRequest;
import com.amorim.finance_manager.creditcard.dto.CreditCardResponse;
import com.amorim.finance_manager.creditcard.dto.UpdateCreditCardRequest;
import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface CreditCardApiDocs {

    @Operation(
            summary = "Criar cartão de crédito",
            description = """
                    Cria um cartão para o usuário autenticado.

                    O limite disponível é inicializado com o mesmo valor
                    do limite total. O cartão é criado com status ACTIVE.

                    A conta padrão precisa pertencer ao usuário autenticado.
                    """,
            requestBody = @RequestBody(
                    required = true,
                    description = "Dados do novo cartão",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation =
                                            CreateCreditCardRequest.class
                            ),
                            examples = @ExampleObject(
                                    name = "Cartão principal",
                                    value = CREATE_CREDIT_CARD_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Cartão criado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreditCardResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = CREDIT_CARD_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados do cartão inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
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
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conta padrão não encontrada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = ACCOUNT_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<CreditCardResponse> create(CreateCreditCardRequest request);

    @Operation(
            summary = "Listar cartões de crédito",
            description = """
                    Lista somente os cartões pertencentes ao usuário
                    autenticado, ordenados pelo nome.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cartões encontrados",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation =
                                                    CreditCardResponse.class
                                    )
                            ),
                            examples = @ExampleObject(
                                    value = CREDIT_CARD_LIST_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<List<CreditCardResponse>> findAll();

    @Operation(
            summary = "Consultar cartão de crédito",
            description = """
                    Retorna um cartão pertencente ao usuário autenticado.
                    Cartões inexistentes ou de outro usuário retornam 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cartão encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreditCardResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = CREDIT_CARD_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cartão não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = CREDIT_CARD_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<CreditCardResponse> findById(
            @Parameter(
                    name = "id",
                    description = "Identificador do cartão",
                    required = true,
                    example = "d89835ee-3463-4a35-a2e9-38d96ab17418",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id
    );

    @Operation(
            summary = "Atualizar cartão de crédito",
            description = """
                    Atualiza parcialmente os dados do cartão.

                    O limite disponível não pode ser enviado diretamente.
                    Ao alterar o limite total, o valor já comprometido
                    permanece preservado.

                    O novo limite não pode ser menor que o valor atualmente
                    comprometido no cartão.
                    """,
            requestBody = @RequestBody(
                    required = true,
                    description = "Campos que serão atualizados",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation =
                                            UpdateCreditCardRequest.class
                            ),
                            examples = @ExampleObject(
                                    name = "Atualização parcial",
                                    value = UPDATE_CREDIT_CARD_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cartão atualizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreditCardResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = UPDATED_CREDIT_CARD_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de atualização inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Validação",
                                            value = VALIDATION_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "PATCH vazio",
                                            value =
                                                    INVALID_CREDIT_CARD_UPDATE
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cartão ou conta padrão não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Cartão não encontrado",
                                            value = CREDIT_CARD_NOT_FOUND
                                    ),
                                    @ExampleObject(
                                            name = "Conta não encontrada",
                                            value = ACCOUNT_NOT_FOUND
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito de limite ou concorrência",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Limite comprometido",
                                            value = CREDIT_LIMIT_CONFLICT
                                    ),
                                    @ExampleObject(
                                            name = "Conflito concorrente",
                                            value =
                                                    OPTIMISTIC_LOCK_CONFLICT
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<CreditCardResponse> update(
            @Parameter(
                    name = "id",
                    description = "Identificador UUID do cartão de crédito",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id,
            UpdateCreditCardRequest request
    );

    @Operation(
            summary = "Registrar compra no cartão",
            description = """
            Registra uma compra à vista ou parcelada no cartão
            do usuário autenticado.

            O limite disponível é reduzido pelo valor total da compra.
            Cada parcela é vinculada à sua fatura mensal correspondente
            e a compra não altera o saldo bancário.
            """,
            requestBody = @RequestBody(
                    required = true,
                    description = "Dados da compra e quantidade de parcelas",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation =
                                            CreateCreditCardPurchaseRequest.class
                            ),
                            examples = @ExampleObject(
                                    value =
                                            CREATE_CREDIT_CARD_PURCHASE_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Compra registrada e parcelas geradas",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation =
                                                    TransactionResponse.class
                                    )
                            ),
                            examples = @ExampleObject(
                                    value = CREDIT_CARD_PURCHASES_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou categoria incompatível",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
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
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cartão ou categoria não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Cartão não encontrado",
                                            value = CREDIT_CARD_NOT_FOUND
                                    ),
                                    @ExampleObject(
                                            name = "Categoria não encontrada",
                                            value = CATEGORY_NOT_FOUND
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        Cartão indisponível, fatura não aberta,
                        limite insuficiente ou conflito concorrente
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Limite insuficiente",
                                            value =
                                                    CREDIT_CARD_PURCHASE_LIMIT_CONFLICT
                                    ),
                                    @ExampleObject(
                                            name = "Cartão indisponível",
                                            value =
                                                    INVALID_CREDIT_CARD_STATUS_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Fatura não aberta",
                                            value =
                                                    INVALID_INVOICE_STATUS_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Conflito concorrente",
                                            value =
                                                    OPTIMISTIC_LOCK_CONFLICT
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<List<TransactionResponse>> createPurchase(
            @Parameter(
                    name = "id",
                    description = "Identificador UUID do cartão de crédito",
                    required = true,
                    example = "d89835ee-3463-4a35-a2e9-38d96ab17418",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id,
            CreateCreditCardPurchaseRequest request
    );
}
