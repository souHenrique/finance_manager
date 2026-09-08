package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.invoice.dto.InvoiceSummaryResponse;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.mapper.InvoiceMapper;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.shared.exception.InvalidInvoiceStatusException;
import com.amorim.finance_manager.shared.exception.InvoiceNotFoundException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceClosingService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final CurrentUserService currentUserService;
    private final Clock financeClock;

    @Transactional
    public InvoiceSummaryResponse close(
            UUID invoiceId,
            Long expectedVersion
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        Invoice invoice = invoiceRepository
                .findOwnedById(invoiceId, userId)
                .orElseThrow(InvoiceNotFoundException::new);

        if (expectedVersion == null || expectedVersion < 0) {
            throw new InvalidInvoiceStatusException(
                    "Informe uma versão válida da fatura"
            );
        }

        if (!Objects.equals(invoice.getVersion(), expectedVersion)) {
            throw new OptimisticLockingFailureException(
                    "A fatura foi alterada. Consulte os dados novamente."
            );
        }

        if (invoice.getStatus() != InvoiceStatus.OPEN
                && invoice.getStatus() != InvoiceStatus.CLOSED) {
            throw new InvalidInvoiceStatusException(
                    "Somente faturas abertas podem ser fechadas"
            );
        }

        if (invoice.getPaidAt() != null) {
            throw new InvalidInvoiceStatusException(
                    "A fatura possui dados de pagamento incompatíveis"
            );
        }

        LocalDate today = LocalDate.now(financeClock);

        if (invoice.getClosingDate() == null
                || !invoice.getClosingDate().isBefore(today)) {
            throw new InvalidInvoiceStatusException(
                    "O dia de fechamento da fatura ainda não terminou"
            );
        }

        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().signum() < 0) {
            throw new InvalidInvoiceStatusException(
                    "A fatura possui total inválido"
            );
        }

        if (invoice.getStatus() == InvoiceStatus.CLOSED) {
            return invoiceMapper.toSummary(invoice);
        }

        invoice.setStatus(InvoiceStatus.CLOSED);

        Invoice saved = invoiceRepository.saveAndFlush(invoice);

        return invoiceMapper.toSummary(saved);
    }
}