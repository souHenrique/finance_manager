package com.amorim.finance_manager.category.api;

import com.amorim.finance_manager.category.dto.CategoryResponse;
import com.amorim.finance_manager.category.dto.CreateCategoryRequest;
import com.amorim.finance_manager.category.dto.UpdateCategoryRequest;
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

public interface CategoryDocsApi {

    @Operation(
            summary = "Criar categoria",
            description = "Cria uma categoria ou subcategoria",
            requestBody =
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CreateCategoryRequest.class
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Categoria",
                                            value = CREATE_CATEGORY_REQUEST
                                    ),
                                    @ExampleObject(
                                            name = "Subcategoria",
                                            value = CREATE_SUBCATEGORY_REQUEST
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Categoria criada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = CategoryResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = CATEGORY_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou tipo incompatível",
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
                    description = "Categoria pai não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = CATEGORY_NOT_FOUND
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
    ResponseEntity<CategoryResponse> create(
            CreateCategoryRequest request
    );

    @Operation(
            summary = "Listar categorias",
            description = "Lista categorias e subcategorias do usuário"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categorias encontradas",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = CategoryResponse.class
                                    )
                            ),
                            examples = @ExampleObject(value = CATEGORY_LIST_RESPONSE)
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
    ResponseEntity<List<CategoryResponse>> findAll();

    @Operation(
            summary = "Consultar categoria",
            description = "Retorna uma categoria ou subcategoria do usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categoria encontrada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = CategoryResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = CATEGORY_RESPONSE
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
                    description = "Categoria não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = CATEGORY_NOT_FOUND
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
    ResponseEntity<CategoryResponse> findById(
            @Parameter(
                    name = "id",
                    description = "Identificador da categoria",
                    required = true,
                    example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id
    );

    @Operation(
            summary = "Atualizar categoria",
            description = "Atualiza parcialmente o nome ou status de uma categoria",
            requestBody =
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = UpdateCategoryRequest.class
                            ),
                            examples = @ExampleObject(
                                    value = UPDATE_CATEGORY_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categoria atualizada",
                    content = @Content(
                            schema = @Schema(
                                    implementation = CategoryResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = UPDATED_CATEGORY_RESPONSE
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
                    description = "Categoria não encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = CATEGORY_NOT_FOUND
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
    ResponseEntity<CategoryResponse> update(
            @Parameter(
                    name = "id",
                    description = "Identificador da categoria",
                    required = true,
                    example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                    schema = @Schema(type = "string", format = "uuid")
            )
            UUID id,
            UpdateCategoryRequest request
    );
}
