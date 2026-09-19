package com.amorim.finance_manager.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Item resumido da listagem de transações")
public record TransactionListItemResponse(

        @Schema(description = "Transação de referência. Em uma compra parcelada, é a primeira parcela.")
        TransactionResponse transaction,

        @Schema(
                description = "Valor exibido na listagem. Para compras parceladas, é o valor integral da compra.",
                example = "1200.00"
        )
        BigDecimal displayAmount,

        @Schema(
                description = "Indica que a compra possui mais de uma parcela e pode ser detalhada.",
                example = "true"
        )
        boolean installmentPurchase
) {

    public static TransactionListItemResponse from(
            TransactionResponse transaction,
            BigDecimal displayAmount
    ) {
        boolean installmentPurchase = transaction.installmentGroupId() != null
                && transaction.installmentCount() != null
                && transaction.installmentCount() > 1;

        return new TransactionListItemResponse(
                transaction,
                displayAmount,
                installmentPurchase
        );
    }
}
