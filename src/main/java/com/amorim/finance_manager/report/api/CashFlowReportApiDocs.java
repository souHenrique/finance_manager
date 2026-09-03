package com.amorim.finance_manager.report.api;

import com.amorim.finance_manager.report.dto.CashFlowReportRequest;
import com.amorim.finance_manager.report.dto.DailyCashFlowResponse;
import com.amorim.finance_manager.report.dto.WeeklyCashFlowResponse;
import com.amorim.finance_manager.shared.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface CashFlowReportApiDocs {

    @Operation(
            summary = "Consultar relatório de caixa diário",
            description = """
                    Retorna o caixa do usuário autenticado no dia informado.

                    Considera somente movimentações COMPLETED cuja effectiveDate
                    corresponda à data consultada.

                    INCOME compõe as entradas. EXPENSE e CREDIT_CARD_PAYMENT
                    compõem as saídas. TRANSFER e CREDIT_CARD_PURCHASE
                    não compõem os totais.

                    invoicePayments já está incluído em outflows.
                    As categorias de despesa incluem somente EXPENSE,
                    sem atribuir pagamentos de fatura às categorias das compras.

                    net representa entradas menos saídas do dia,
                    não o saldo atual das contas.

                    Sem movimentações elegíveis, retorna HTTP 200,
                    valores zerados e listas de categorias vazias.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório diário calculado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = DailyCashFlowResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Relatório diário",
                                    value = DAILY_CASH_FLOW_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Data obrigatória ausente, formato de data inválido
                            ou período fora do intervalo permitido.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Data ausente",
                                            value = DAILY_CASH_DATE_REQUIRED_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Período inválido",
                                            value = DAILY_CASH_INVALID_PERIOD_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticação ausente ou token inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = DAILY_CASH_UNAUTHORIZED_ERROR
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
                                    value = DAILY_CASH_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<DailyCashFlowResponse> daily(@ParameterObject CashFlowReportRequest request);

    @Operation(
            summary = "Consultar relatório de caixa semanal",
            description = """
                    Retorna o caixa da semana que contém a data informada,
                    com comparação à semana imediatamente anterior.

                    Cada semana vai de segunda-feira a domingo, inclusive.
                    Para date=2026-09-03, a semana consultada vai de
                    2026-08-31 a 2026-09-06; a anterior vai de
                    2026-08-24 a 2026-08-30.

                    Considera somente movimentações COMPLETED do usuário
                    autenticado, selecionadas pela effectiveDate.

                    INCOME compõe as entradas. EXPENSE e CREDIT_CARD_PAYMENT
                    compõem as saídas. TRANSFER e CREDIT_CARD_PURCHASE
                    não compõem os totais.

                    invoicePayments já está incluído em outflows.
                    As categorias de despesa incluem somente EXPENSE.

                    As diferenças representam semana consultada menos
                    semana anterior, em valores monetários, não percentuais.
                    net não representa o saldo atual das contas.

                    Sem movimentações elegíveis em uma semana, seu resumo
                    contém valores zerados e listas de categorias vazias.

                    Todo o intervalo calculado, incluindo a semana anterior,
                    deve estar entre 0001-01-01 e 9999-12-31.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório semanal e comparação calculados",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = WeeklyCashFlowResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Relatório semanal",
                                    value = WEEKLY_CASH_FLOW_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Data obrigatória ausente, formato de data inválido
                            ou intervalo calculado fora dos limites permitidos.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Data ausente",
                                            value = WEEKLY_CASH_DATE_REQUIRED_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Período inválido",
                                            value = WEEKLY_CASH_INVALID_PERIOD_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticação ausente ou token inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = WEEKLY_CASH_UNAUTHORIZED_ERROR
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
                                    value = WEEKLY_CASH_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<WeeklyCashFlowResponse> weekly(@ParameterObject CashFlowReportRequest request);
}
