package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(
        description = """
                Resumo das movimentações COMPLETED no período,
                considerando a data de efetivação.
                Transferências e compras no cartão não compõem os totais.
                """
)
public record CashFlowSummaryResponse(

        @Schema(
                description = "Total de entradas efetivas do tipo INCOME",
                example = "5000.00"
        )
        BigDecimal inflows,

        @Schema(
                description = """
                        Total de saídas efetivas.
                        Soma das despesas EXPENSE e dos pagamentos
                        de fatura CREDIT_CARD_PAYMENT.
                        """,
                example = "1500.00"
        )
        BigDecimal outflows,

        @Schema(
                description = """
                        Resultado líquido: inflows menos outflows.
                        Pode ser negativo. Não representa o saldo atual das contas.
                        """,
                example = "3500.00"
        )
        BigDecimal net,

        @Schema(
                description = """
                        Total de pagamentos efetivos de fatura.
                        Este valor já está incluído em outflows;
                        não deve ser somado novamente.
                        """,
                example = "1200.00"
        )
        BigDecimal invoicePayments,

        @Schema(
                description = """
                        Receitas agrupadas por categoria, em ordem decrescente de valor.
                        Subcategorias permanecem separadas das categorias pai.
                        Retorna lista vazia quando não houver receitas.
                        """
        )
        List<CategoryCashFlowResponse> incomeCategories,

        @Schema(
                description = """
                        Despesas EXPENSE agrupadas por categoria,
                        em ordem decrescente de valor.
                        Não inclui pagamentos de fatura nem compras no cartão.
                        Subcategorias permanecem separadas das categorias pai.
                        Retorna lista vazia quando não houver despesas diretas.
                        """
        )
        List<CategoryCashFlowResponse> expenseCategories
) {
}
