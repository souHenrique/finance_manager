package com.amorim.finance_manager.transfer.api;

import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transfer.dto.CreateTransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface TransferApiDocs {

    @Operation(
            summary = "Transferir entre contas",
            description = """
                    Debita a conta de origem e credita a conta de destino
                    dentro da mesma transação de banco de dados.
                    """,
            requestBody =
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreateTransferRequest.class
                            ),
                            examples = @ExampleObject(
                                    value = CREATE_TRANSFER_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transferência concluída",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TransactionResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = TRANSFER_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados ou regra da transferência inválidos",
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
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conta de origem ou destino não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = ACCOUNT_NOT_FOUND
                            )
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
    ResponseEntity<TransactionResponse> transfer(
            CreateTransferRequest request
    );
}
