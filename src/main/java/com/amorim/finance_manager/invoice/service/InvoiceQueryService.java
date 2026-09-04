package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.dto.InvoiceDetailResponse;
import com.amorim.finance_manager.invoice.dto.InvoiceFilterRequest;
import com.amorim.finance_manager.invoice.dto.InvoiceSummaryResponse;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.mapper.InvoiceMapper;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.invoice.specification.InvoiceSpecifications;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.InvoiceNotFoundException;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceMapper invoiceMapper;
    private final TransactionMapper transactionMapper;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> list(
            InvoiceFilterRequest filters,
            Pageable pageable
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return listInternal(userId, filters, pageable);
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse findById(UUID invoiceId) {
        UUID userId = currentUserService.getCurrentUserId();

        Invoice invoice = invoiceRepository
                .findOwnedById(invoiceId, userId)
                .orElseThrow(InvoiceNotFoundException::new);

        List<TransactionResponse> transactions = transactionRepository
                .findAllByInvoiceIdAndUserIdOrderByCompetenceDateAscCreatedAtAsc(
                        invoiceId,
                        userId
                )
                .stream()
                .map(transactionMapper::toResponse)
                .toList();

        return invoiceMapper.toDetail(invoice, transactions);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> listByCreditCard(
            UUID creditCardId,
            Integer referenceYear,
            Integer referenceMonth,
            InvoiceStatus status,
            Pageable pageable
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        creditCardRepository
                .findByIdAndUserId(creditCardId, userId)
                .orElseThrow(CreditCardNotFoundException::new);

        InvoiceFilterRequest filters = new InvoiceFilterRequest(
                creditCardId,
                referenceMonth,
                referenceYear,
                status
        );

        return listInternal(userId, filters, pageable);
    }

    private Page<InvoiceSummaryResponse> listInternal(
            UUID userId,
            InvoiceFilterRequest filters,
            Pageable pageable
    ) {
        return invoiceRepository
                .findAll(
                        InvoiceSpecifications.from(userId, filters),
                        pageable
                )
                .map(invoiceMapper::toSummary);
    }
}
