package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceCycleServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111"
    );
    private static final UUID CREDIT_CARD_ID = UUID.fromString(
            "22222222-2222-2222-2222-222222222222"
    );
    private static final UUID INVOICE_ID = UUID.fromString(
            "33333333-3333-3333-3333-333333333333"
    );

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private InvoiceCycleService service;

    @ParameterizedTest
    @CsvSource({
            "2026-09-09, 9, 2026, 2026-09-10, 2026-09-17",
            "2026-09-10, 9, 2026, 2026-09-10, 2026-09-17",
            "2026-09-11, 10, 2026, 2026-10-10, 2026-10-17",
            "2026-01-31, 2, 2026, 2026-02-10, 2026-02-17",
            "2026-12-11, 1, 2027, 2027-01-10, 2027-01-17"
    })
    void shouldResolveBeforeOnAndAfterClosingIncludingMonthAndYearRollover(
            String purchaseDate,
            int expectedMonth,
            int expectedYear,
            String expectedClosingDate,
            String expectedDueDate
    ) {
        InvoiceCycle cycle = service.calculate(
                LocalDate.parse(purchaseDate),
                10,
                17
        );

        assertThat(cycle.referenceMonth()).isEqualTo(expectedMonth);
        assertThat(cycle.referenceYear()).isEqualTo(expectedYear);
        assertThat(cycle.closingDate()).isEqualTo(expectedClosingDate);
        assertThat(cycle.dueDate()).isEqualTo(expectedDueDate);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-02-01, 28, 2026-02-28",
            "2026-02-01, 29, 2026-02-28",
            "2028-02-01, 29, 2028-02-29",
            "2026-02-01, 30, 2026-02-28",
            "2026-04-01, 31, 2026-04-30"
    })
    void shouldUseTheLastValidClosingDayForDaysFromTwentyEightToThirtyOne(
            String purchaseDate,
            int closingDay,
            String expectedClosingDate
    ) {
        InvoiceCycle cycle = service.calculate(
                LocalDate.parse(purchaseDate),
                closingDay,
                5
        );

        assertThat(cycle.closingDate()).isEqualTo(expectedClosingDate);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-09-01, 10, 17, 2026-09-17",
            "2026-02-01, 25, 31, 2026-02-28",
            "2026-02-01, 31, 31, 2026-03-31",
            "2026-04-01, 30, 31, 2026-05-31",
            "2028-02-01, 28, 29, 2028-02-29"
    })
    void shouldUseTheFirstValidDueDateStrictlyAfterClosing(
            String purchaseDate,
            int closingDay,
            int dueDay,
            String expectedDueDate
    ) {
        InvoiceCycle cycle = service.calculate(
                LocalDate.parse(purchaseDate),
                closingDay,
                dueDay
        );

        assertThat(cycle.dueDate()).isEqualTo(expectedDueDate);
        assertThat(cycle.dueDate()).isAfter(cycle.closingDate());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 17",
            "32, 17",
            "10, 0",
            "10, 32"
    })
    void shouldRejectInvalidConfiguredDays(int closingDay, int dueDay) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.calculate(
                        LocalDate.of(2026, 9, 1),
                        closingDay,
                        dueDay
                ))
                .withMessage("Dia deve estar entre 1 e 31");
    }

    @Test
    void shouldRejectMissingPurchaseDate() {
        assertThatNullPointerException()
                .isThrownBy(() -> service.calculate(null, 10, 17))
                .withMessage("Data da compra é obrigatória");
    }

    @Test
    void shouldReturnTheExistingInvoiceWithoutCreatingADuplicate() {
        CreditCard card = card(10, 17);
        Invoice existing = invoice(9, 2026);
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CREDIT_CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(invoiceRepository.findByCreditCardIdAndReferenceMonthAndReferenceYear(
                CREDIT_CARD_ID,
                9,
                2026
        )).thenReturn(Optional.of(existing));

        Invoice result = service.findOrCreate(
                CREDIT_CARD_ID,
                LocalDate.of(2026, 9, 9)
        );

        assertThat(result).isSameAs(existing);
        verify(invoiceRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldCreateAnOpenZeroValueInvoiceForTheCalculatedCycle() {
        CreditCard card = card(10, 17);
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CREDIT_CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(invoiceRepository.findByCreditCardIdAndReferenceMonthAndReferenceYear(
                CREDIT_CARD_ID,
                10,
                2026
        )).thenReturn(Optional.empty());
        when(invoiceRepository.saveAndFlush(any(Invoice.class)))
                .thenAnswer(invocation -> {
                    Invoice saved = invocation.getArgument(0);
                    saved.setId(INVOICE_ID);
                    saved.setVersion(0L);
                    return saved;
                });

        Invoice result = service.findOrCreate(
                CREDIT_CARD_ID,
                LocalDate.of(2026, 9, 11)
        );

        assertThat(result.getId()).isEqualTo(INVOICE_ID);
        assertThat(result.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(result.getReferenceMonth()).isEqualTo(10);
        assertThat(result.getReferenceYear()).isEqualTo(2026);
        assertThat(result.getClosingDate()).isEqualTo("2026-10-10");
        assertThat(result.getDueDate()).isEqualTo("2026-10-17");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(result.getPaidAt()).isNull();
        assertThat(result.getVersion()).isZero();
    }

    @Test
    void shouldHideMissingOrForeignCreditCardsAsNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CREDIT_CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOrCreate(
                CREDIT_CARD_ID,
                LocalDate.of(2026, 9, 1)
        )).isInstanceOf(CreditCardNotFoundException.class);

        verify(creditCardRepository).findByIdAndUserId(CREDIT_CARD_ID, USER_ID);
        verifyNoInteractions(invoiceRepository);
    }

    private CreditCard card(int closingDay, int dueDay) {
        CreditCard card = new CreditCard();
        card.setId(CREDIT_CARD_ID);
        card.setClosingDay(closingDay);
        card.setDueDay(dueDay);
        return card;
    }

    private Invoice invoice(int referenceMonth, int referenceYear) {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setCreditCardId(CREDIT_CARD_ID);
        invoice.setReferenceMonth(referenceMonth);
        invoice.setReferenceYear(referenceYear);
        invoice.setClosingDate(LocalDate.of(referenceYear, referenceMonth, 10));
        invoice.setDueDate(LocalDate.of(referenceYear, referenceMonth, 17));
        invoice.setTotalAmount(BigDecimal.ZERO.setScale(2));
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setVersion(0L);
        return invoice;
    }
}
