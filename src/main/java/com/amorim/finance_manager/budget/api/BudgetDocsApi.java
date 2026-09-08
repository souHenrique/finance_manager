package com.amorim.finance_manager.budget.api;

import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.dto.CreateBudgetRequest;
import com.amorim.finance_manager.budget.dto.UpdateBudgetRequest;
import com.amorim.finance_manager.shared.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface BudgetDocsApi {

    @Operation(
            summary = "Criar orçamento",
            description = """
                    Cria um orçamento mensal para uma categoria de despesa
                    pertencente ao usuário autenticado.

                    Só pode existir um orçamento por usuário, categoria,
                    mês e ano.
                    """,
            requestBody = @RequestBody(
                    required = true,
                    description = "Dados do novo orçamento",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreateBudgetRequest.class
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Orçamento criado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BudgetResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Dados inválidos ou categoria incompatível.
                            Somente categorias de despesa são aceitas.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Categoria não encontrada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Já existe um orçamento para a categoria
                            no mês e ano informados.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<BudgetResponse> create(CreateBudgetRequest request);

    @Operation(
            summary = "Consultar orçamento",
            description = """
                    Retorna um orçamento pertencente ao usuário autenticado.
                    Orçamentos inexistentes ou de outro usuário retornam 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Orçamento encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BudgetResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Orçamento não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<BudgetResponse> findById(
            @Parameter(
                    name = "id",
                    description = "Identificador do orçamento",
                    required = true,
                    example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id
    );

    @Operation(
            summary = "Atualizar orçamento",
            description = """
                    Atualiza parcialmente um orçamento do usuário autenticado.

                    Ao alterar categoria, mês ou ano, a combinação resultante
                    não pode pertencer a outro orçamento.
                    Ao menos um campo deve ser enviado.
                    """,
            requestBody = @RequestBody(
                    required = true,
                    description = "Campos que devem ser atualizados",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = UpdateBudgetRequest.class
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Orçamento atualizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BudgetResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Atualização vazia, dados inválidos ou
                            categoria incompatível.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Orçamento ou categoria não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            A combinação de categoria, mês e ano
                            já pertence a outro orçamento.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<BudgetResponse> update(
            @Parameter(
                    name = "id",
                    description = "Identificador do orçamento",
                    required = true,
                    example = "c487c4cf-d948-4ennials-b85f-e36bb798c928",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id,
            UpdateBudgetRequest request
    );

    @Operation(
            summary = "Excluir orçamento",
            description = """
                    Exclui definitivamente um orçamento pertencente
                    ao usuário autenticado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Orçamento excluído"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Orçamento não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<Void> delete(
            @Parameter(
                    name = "id",
                    description = "Identificador do orçamento",
                    required = true,
                    example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id
    );
}
