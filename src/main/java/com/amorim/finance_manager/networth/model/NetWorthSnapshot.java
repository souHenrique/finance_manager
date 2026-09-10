package com.amorim.finance_manager.networth.model;

import java.math.BigDecimal;

public record NetWorthSnapshot(
        BigDecimal consolidatedBalance,
        BigDecimal unpaidInvoices,
        BigDecimal netWorth
) {
}
