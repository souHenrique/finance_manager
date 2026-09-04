package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.dto.InvoiceDetailResponse;
import com.amorim.finance_manager.invoice.dto.InvoiceFilterRequest;
import com.amorim.finance_manager.invoice.dto.InvoiceSummaryResponse;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.mapper.InvoiceMapper;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.InvoiceNotFoundException;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceQueryServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CARD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INVOICE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private CurrentUserService currentUserService;
    @InjectMocks
    private InvoiceQueryService invoiceQueryService;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void shouldListOwnedInvoicesAndMapOnlySummaries() {
        Invoice invoice = invoice();
        InvoiceSummaryResponse response = summary();
        Pageable pageable = PageRequest.of(1, 5);
        InvoiceFilterRequest filters = new InvoiceFilterRequest(
                CARD_ID,
                9,
                2026,
                InvoiceStatus.OPEN
        );
        when(invoiceRepository.findAll(
                ArgumentMatchers.<Specification<Invoice>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(invoice), pageable, 12));
        when(invoiceMapper.toSummary(invoice)).thenReturn(response);

        Page<InvoiceSummaryResponse> result = invoiceQueryService.list(filters, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(12);
        verify(currentUserService).getCurrentUserId();
        verify(invoiceRepository).findAll(
                ArgumentMatchers.<Specification<Invoice>>any(),
                eq(pageable)
        );
        verify(invoiceMapper).toSummary(invoice);
        verifyNoInteractions(creditCardRepository, transactionRepository, transactionMapper);
    }

    @Test
    void shouldReturnOwnedInvoiceWithMappedTransactions() {
        Invoice invoice = invoice();
        Transaction first = transaction();
        Transaction second = transaction();
        TransactionResponse firstResponse = mock(TransactionResponse.class);
        TransactionResponse secondResponse = mock(TransactionResponse.class);
        InvoiceDetailResponse expected = mock(InvoiceDetailResponse.class);
        List<Transaction> transactions = List.of(first, second);
        List<TransactionResponse> transactionResponses = List.of(firstResponse, secondResponse);
        when(invoiceRepository.findOwnedById(INVOICE_ID, USER_ID))
                .thenReturn(Optional.of(invoice));
        when(transactionRepository.findAllByInvoiceIdAndUserIdOrderByCompetenceDateAscCreatedAtAsc(
                INVOICE_ID,
                USER_ID
        )).thenReturn(transactions);
        when(transactionMapper.toResponse(first)).thenReturn(firstResponse);
        when(transactionMapper.toResponse(second)).thenReturn(secondResponse);
        when(invoiceMapper.toDetail(invoice, transactionResponses)).thenReturn(expected);

        InvoiceDetailResponse result = invoiceQueryService.findById(INVOICE_ID);

        assertThat(result).isSameAs(expected);
        verify(invoiceRepository).findOwnedById(INVOICE_ID, USER_ID);
        verify(transactionRepository).findAllByInvoiceIdAndUserIdOrderByCompetenceDateAscCreatedAtAsc(
                INVOICE_ID,
                USER_ID
        );
        verify(transactionMapper).toResponse(first);
        verify(transactionMapper).toResponse(second);
        verify(invoiceMapper).toDetail(invoice, transactionResponses);
        verifyNoInteractions(creditCardRepository);
    }

    @Test
    void shouldHideMissingOrForeignInvoiceAsNotFoundWithoutLoadingTransactions() {
        when(invoiceRepository.findOwnedById(INVOICE_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceQueryService.findById(INVOICE_ID))
                .isInstanceOf(InvoiceNotFoundException.class)
                .hasMessage("Fatura não encontrada");

        verify(invoiceRepository).findOwnedById(INVOICE_ID, USER_ID);
        verifyNoInteractions(transactionRepository, transactionMapper, invoiceMapper, creditCardRepository);
    }

    @Test
    void shouldValidateCardOwnershipBeforeListingItsInvoices() {
        CreditCard card = new CreditCard();
        Pageable pageable = PageRequest.of(0, 20);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(invoiceRepository.findAll(
                ArgumentMatchers.<Specification<Invoice>>any(),
                eq(pageable)
        )).thenReturn(Page.empty(pageable));

        Page<InvoiceSummaryResponse> result = invoiceQueryService.listByCreditCard(
                CARD_ID,
                2026,
                9,
                InvoiceStatus.CLOSED,
                pageable
        );

        assertThat(result).isEmpty();
        verify(creditCardRepository).findByIdAndUserId(CARD_ID, USER_ID);
        verify(invoiceRepository).findAll(
                ArgumentMatchers.<Specification<Invoice>>any(),
                eq(pageable)
        );
        verifyNoInteractions(transactionRepository, transactionMapper);
    }

    @Test
    void shouldHideMissingOrForeignCardAndSkipInvoiceQuery() {
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceQueryService.listByCreditCard(
                CARD_ID,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        )).isInstanceOf(CreditCardNotFoundException.class)
                .hasMessage("Cartão de crédito não encontrado");

        verify(creditCardRepository).findByIdAndUserId(CARD_ID, USER_ID);
        verifyNoInteractions(invoiceRepository, invoiceMapper, transactionRepository, transactionMapper);
    }

    private Invoice invoice() {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setCreditCardId(CARD_ID);
        invoice.setReferenceMonth(9);
        invoice.setReferenceYear(2026);
        invoice.setClosingDate(LocalDate.of(2026, 9, 10));
        invoice.setDueDate(LocalDate.of(2026, 9, 17));
        invoice.setTotalAmount(new BigDecimal("250.00"));
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setVersion(0L);
        return invoice;
    }

    private InvoiceSummaryResponse summary() {
        return new InvoiceSummaryResponse(
                INVOICE_ID,
                CARD_ID,
                9,
                2026,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 17),
                new BigDecimal("250.00"),
                InvoiceStatus.OPEN,
                null,
                0L
        );
    }

    private Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.setUserId(USER_ID);
        transaction.setInvoiceId(INVOICE_ID);
        return transaction;
    }
}
