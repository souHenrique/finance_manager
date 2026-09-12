package com.amorim.finance_manager.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Regime utilizado no cálculo do indicador")
public enum AccountingBasis {
    CASH,
    COMPETENCE
}
