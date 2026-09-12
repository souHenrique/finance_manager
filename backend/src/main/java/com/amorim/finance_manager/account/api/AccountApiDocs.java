package com.amorim.finance_manager.account.api;

import com.amorim.finance_manager.account.dto.AccountResponse;
import com.amorim.finance_manager.account.dto.CreateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountRequest;
import com.amorim.finance_manager.account.dto.UpdateAccountStatusRequest;
import com.amorim.finance_manager.shared.exception.ApiError;
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

public interface AccountApiDocs {

    @Operation(
            summary = "Criar conta",
            description = "Cria uma conta para o usuário autenticado",
            requestBody = @RequestBody(
                    required = true,
                    description = "Dados da nova conta",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreateAccountRequest.class
                            ),
                            examples = @ExampleObject(
                                    name = "Conta corrente",
                                    value = CREATE_ACCOUNT_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Conta criada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = AccountResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = ACCOUNT_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class
                            ),
                            examples = @ExampleObject(
                                    value = VALIDATION_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class
                            ),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class
                            ),
                            examples = @ExampleObject(
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<AccountResponse> create(
            CreateAccountRequest request
    );

    @Operation(
            summary = "Listar contas",
            description = "Lista as contas pertencentes ao usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contas encontradas",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = AccountResponse.class
                                    )
                            ),
                            examples = @ExampleObject(value = ACCOUNT_LIST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class
                            ),
                            examples = @ExampleObject(value = UNAUTHORIZED_ERROR)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class
                            ),
                            examples = @ExampleObject(value = INTERNAL_SERVER_ERROR)
                    )
            )
    })
    ResponseEntity<List<AccountResponse>> findAll();

    @Operation(
            summary = "Consultar conta",
            description = "Retorna uma conta pertencente ao usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conta encontrada",
                    content = @Content(
                            schema = @Schema(implementation = AccountResponse.class),
                            examples = @ExampleObject(value = ACCOUNT_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = UNAUTHORIZED_ERROR)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conta não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ACCOUNT_NOT_FOUND)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = INTERNAL_SERVER_ERROR)
                    )
            )
    })
    ResponseEntity<AccountResponse> findById(
            @Parameter(
                    name = "id",
                    description = "Identificador da conta",
                    required = true,
                    example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id);

    @Operation(
            summary = "Atualizar conta",
            description = "Atualiza parcialmente os dados da conta",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = UpdateAccountRequest.class
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Atualização completa",
                                            value = UPDATE_ACCOUNT_REQUEST
                                    ),
                                    @ExampleObject(
                                            name = "Somente nome",
                                            value = PARTIAL_ACCOUNT_UPDATE_REQUEST
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conta atualizada",
                    content = @Content(
                            schema = @Schema(implementation = AccountResponse.class),
                            examples = @ExampleObject(value = UPDATED_ACCOUNT_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de atualização inválidos",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = VALIDATION_ERROR)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = UNAUTHORIZED_ERROR)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conta não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ACCOUNT_NOT_FOUND)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito de atualização concorrente",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = OPTIMISTIC_LOCK_CONFLICT)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = INTERNAL_SERVER_ERROR)
                    )
            )
    })
    ResponseEntity<AccountResponse> update(
            @Parameter(
                    name = "id",
                    description = "Identificador da conta",
                    required = true,
                    example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id,
            UpdateAccountRequest request
    );

    @Operation(
            summary = "Alterar status da conta",
            description = "Ativa ou inativa uma conta",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = UpdateAccountStatusRequest.class
                            ),
                            examples = @ExampleObject(
                                    name = "Inativar conta",
                                    value = UPDATE_ACCOUNT_STATUS_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status alterado",
                    content = @Content(
                            schema = @Schema(implementation = AccountResponse.class),
                            examples = @ExampleObject(value = INACTIVE_ACCOUNT_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Status não informado ou inválido",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = VALIDATION_ERROR)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = UNAUTHORIZED_ERROR)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conta não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ACCOUNT_NOT_FOUND)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito de atualização concorrente",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = OPTIMISTIC_LOCK_CONFLICT)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = INTERNAL_SERVER_ERROR)
                    )
            )
    })
    ResponseEntity<AccountResponse> updateStatus(
            @Parameter(
                    name = "id",
                    description = "Identificador da conta",
                    required = true,
                    example = "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id,
            UpdateAccountStatusRequest request
    );
}
