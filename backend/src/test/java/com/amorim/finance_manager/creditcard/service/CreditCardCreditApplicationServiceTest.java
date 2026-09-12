package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.creditcard.entity.CreditCardCredit;
import com.amorim.finance_manager.creditcard.entity.CreditCardCreditApplication;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditApplicationRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardCreditApplicationServiceTest {
    @Mock CreditCardCreditRepository credits;
    @Mock CreditCardCreditApplicationRepository applications;
    @InjectMocks CreditCardCreditApplicationService service;
    @Captor ArgumentCaptor<List<CreditCardCreditApplication>> applicationCaptor;
    private final UUID userId = UUID.randomUUID();
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCreditCardId(UUID.randomUUID());
        invoice.setReferenceMonth(1);
        invoice.setReferenceYear(2027);
    }

    @Test
    void shouldRejectNullAndNegativeDueBeforeAccessingRepositories() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.apply(userId, invoice, null));
        assertThatIllegalArgumentException().isThrownBy(() -> service.apply(userId, invoice, new BigDecimal("-0.01")));
        verifyNoInteractions(credits, applications);
    }

    @Test
    void shouldNotQueryOrCreateZeroValueApplicationsForZeroDue() {
        assertThat(service.apply(userId, invoice, BigDecimal.ZERO)).isZero();
        verifyNoInteractions(credits, applications);
    }

    @Test
    void shouldReturnZeroWhenNoOwnedEligibleCreditsExist() {
        when(eligible()).thenReturn(List.of());
        assertThat(service.apply(userId, invoice, new BigDecimal("100"))).isZero();
        verify(credits).findEligibleCredits(userId, invoice.getCreditCardId(), 2027, 1, PageRequest.of(0, 100));
        verifyNoInteractions(applications);
    }

    @Test
    void shouldConsumeInRepositoryOrderPreservingRemainderAndUnusedCredits() {
        CreditCardCredit first = credit("30.00"), second = credit("90.00"), unused = credit("50.00");
        when(eligible()).thenReturn(List.of(first, second, unused));
        assertThat(service.apply(userId, invoice, new BigDecimal("100.00"))).isEqualByComparingTo("100");
        assertThat(first.getRemainingAmount()).isZero();
        assertThat(second.getRemainingAmount()).isEqualByComparingTo("20");
        assertThat(unused.getRemainingAmount()).isEqualByComparingTo("50");
        verify(credits).saveAllAndFlush(List.of(first, second));
        verify(applications).saveAllAndFlush(applicationCaptor.capture());
        List<CreditCardCreditApplication> rows = applicationCaptor.getValue();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(CreditCardCreditApplication::getCreditId).containsExactly(first.getId(), second.getId());
        assertThat(rows).allSatisfy(row -> assertThat(row.getInvoiceId()).isEqualTo(invoice.getId()));
        assertThat(rows.get(0).getAmount()).isEqualByComparingTo("30");
        assertThat(rows.get(1).getAmount()).isEqualByComparingTo("70");
    }

    @Test
    void shouldReturnOnlyCreditAppliedWhenCreditIsInsufficient() {
        CreditCardCredit credit = credit("25.37");
        when(eligible()).thenReturn(List.of(credit), List.of());
        assertThat(service.apply(userId, invoice, new BigDecimal("100"))).isEqualByComparingTo("25.37");
        assertThat(credit.getRemainingAmount()).isZero();
        verify(credits, times(2)).findEligibleCredits(userId, invoice.getCreditCardId(), 2027, 1, PageRequest.of(0, 100));
    }

    @Test
    void shouldProcessMoreThanOneHundredCreditsWithoutSkippingAfterConsumption() {
        List<CreditCardCredit> firstBatch = IntStream.range(0, 100).mapToObj(i -> credit("1.00")).toList();
        CreditCardCredit last = credit("2.00");
        when(eligible()).thenReturn(firstBatch, List.of(last));
        assertThat(service.apply(userId, invoice, new BigDecimal("101.50"))).isEqualByComparingTo("101.50");
        assertThat(firstBatch).allSatisfy(credit -> assertThat(credit.getRemainingAmount()).isZero());
        assertThat(last.getRemainingAmount()).isEqualByComparingTo("0.50");
        verify(credits, times(2)).findEligibleCredits(userId, invoice.getCreditCardId(), 2027, 1, PageRequest.of(0, 100));
        verify(applications, times(2)).saveAllAndFlush(applicationCaptor.capture());
        assertThat(applicationCaptor.getAllValues().get(0)).hasSize(100);
        assertThat(applicationCaptor.getAllValues().get(1)).hasSize(1);
    }

    @Test
    void shouldPropagateOptimisticConflictWithoutRecordingApplications() {
        when(eligible()).thenReturn(List.of(credit("100")));
        doThrow(new OptimisticLockingFailureException("Concurrent credit consumption"))
                .when(credits).saveAllAndFlush(anyList());
        assertThatThrownBy(() -> service.apply(userId, invoice, new BigDecimal("100")))
                .isInstanceOf(OptimisticLockingFailureException.class);
        verifyNoInteractions(applications);
    }

    private List<CreditCardCredit> eligible() {
        return credits.findEligibleCredits(userId, invoice.getCreditCardId(), 2027, 1, PageRequest.of(0, 100));
    }

    private CreditCardCredit credit(String amount) {
        CreditCardCredit credit = new CreditCardCredit();
        credit.setId(UUID.randomUUID());
        credit.setUserId(userId);
        credit.setCreditCardId(invoice.getCreditCardId());
        credit.setOriginalAmount(new BigDecimal(amount));
        credit.setRemainingAmount(new BigDecimal(amount));
        return credit;
    }
}
