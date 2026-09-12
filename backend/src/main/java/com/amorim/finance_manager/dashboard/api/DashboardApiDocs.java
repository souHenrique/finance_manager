package com.amorim.finance_manager.dashboard.api;

import com.amorim.finance_manager.dashboard.dto.DashboardResponse;
import com.amorim.finance_manager.shared.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface DashboardApiDocs {

    @Operation(
            summary = "Consultar o dashboard financeiro",
            description = """
                    Retorna os indicadores financeiros do usuário autenticado.

                    referenceDate utiliza o fuso horário configurado pela aplicação.
                    Entradas, saídas, despesas por competência e orçamento
                    representam o mês que contém referenceDate.

                    consolidatedBalance soma os saldos atuais das contas.

                    monthlyInflows e cashOutflows utilizam o regime CASH e
                    selecionam transações pela effectiveDate. Pagamentos de
                    fatura compõem cashOutflows; compras no cartão não.

                    competenceExpenses utiliza o regime COMPETENCE e seleciona
                    transações pela competenceDate. Compras no cartão compõem
                    esse indicador; pagamentos de fatura não.

                    openInvoices soma exclusivamente faturas com status OPEN.
                    Faturas CLOSED não aparecem nesse indicador, mas continuam
                    sendo descontadas do patrimônio enquanto não forem pagas.

                    budget considera os orçamentos do mês atual e seu consumo
                    por competenceDate.

                    netWorth corresponde ao saldo consolidado menos as faturas
                    OPEN e CLOSED. Faturas PAID e CANCELLED não são descontadas.

                    Cada indicador informa explicitamente seu regime por meio
                    do campo basis.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard calculado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = DashboardResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Dashboard financeiro",
                                    value = DASHBOARD_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticação ausente ou token inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = DASHBOARD_UNAUTHORIZED_ERROR
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
                                    value = DASHBOARD_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<DashboardResponse> get();
}
