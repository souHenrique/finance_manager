package com.amorim.finance_manager.user.api;

import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.user.dto.AuthResponse;
import com.amorim.finance_manager.user.dto.LoginRequest;
import com.amorim.finance_manager.user.dto.RegisterRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
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

public interface AuthApiDocs {

    @Operation(
            summary = "Cadastrar usuário",
            description = "Cadastra um usuário e armazena sua senha utilizando BCrypt",
            requestBody =
            @RequestBody(
                    required = true,
                    description = "Dados do novo usuário",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = RegisterRequest.class
                            ),
                            examples = @ExampleObject(
                                    name = "Cadastro válido",
                                    value = REGISTER_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário cadastrado",
                    content = @Content(
                            schema = @Schema(
                                    implementation = UserResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = USER_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de cadastro inválidos",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = VALIDATION_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Limite de tentativas de cadastro excedido",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = EMAIL_ALREADY_EXISTS
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<UserResponse> register(RegisterRequest request);

    @Operation(
            summary = "Realizar login",
            description = "Autentica o usuário e grava a sessão no cookie HttpOnly nummo_session",
            requestBody =
            @RequestBody(
                    required = true,
                    description = "Credenciais do usuário",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = LoginRequest.class
                            ),
                            examples = @ExampleObject(
                                    value = LOGIN_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado",
                    content = @Content(
                            schema = @Schema(
                                    implementation = AuthResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = AUTH_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de login inválidos",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = VALIDATION_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Limite de tentativas de login excedido",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = INVALID_CREDENTIALS_ERROR
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
    ResponseEntity<AuthResponse> login(LoginRequest request);

    @Operation(summary = "Encerrar sessão", description = "Remove o cookie de sessão HttpOnly")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessão encerrada"),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    ResponseEntity<Void> logout();
}
