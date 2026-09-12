package com.amorim.finance_manager.dashboard.dto;

import com.amorim.finance_manager.budget.model.BudgetAlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Consumo de um orçamento por categoria")
public record DashboardBudgetItemResponse(

        @Schema(
                description = "Identificador do orçamento",
                example = "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                format = "uuid"
        )
        UUID budgetId,

        @Schema(
                description = "Categoria de despesa do orçamento",
                example = "57b1879c-a98e-4718-b66d-47f970ab6709",
                format = "uuid"
        )
        UUID categoryId,

        @Schema(
                description = "Limite definido para a categoria",
                example = "1500.00"
        )
        BigDecimal amountLimit,

        @Schema(
                description = "Total consumido pela categoria no mês",
                example = "1200.00"
        )
        BigDecimal spentAmount,

        @Schema(
                description = "Percentual consumido do limite",
                example = "80.00"
        )
        BigDecimal usagePercentage,

        @Schema(
                description = "Situação atual do orçamento",
                example = "ALERT"
        )
        BudgetAlertStatus alertStatus
) {
}
