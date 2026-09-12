package com.amorim.finance_manager.networth.service;

import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.networth.model.NetWorthSnapshot;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class NetWorthService {

    private static final Set<InvoiceStatus> UNPAID_INVOICE_STATUSES = Set.of(
            InvoiceStatus.OPEN,
            InvoiceStatus.CLOSED
    );

    private final AccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;

    public BigDecimal calculate() {
        return calculateSnapshot().netWorth();
    }

    public NetWorthSnapshot calculateSnapshot() {
        UUID userId = currentUserService.getCurrentUserId();

        BigDecimal accountBalances = accountRepository.sumCurrentBalanceByUserId(userId);

        BigDecimal unpaidInvoices = invoiceRepository.sumTotalAmountOwnedByUserIdAndStatusIn(
                userId,
                UNPAID_INVOICE_STATUSES
        );

        return new NetWorthSnapshot(
                accountBalances,
                unpaidInvoices,
                accountBalances.subtract(unpaidInvoices)
        );
    }
}
