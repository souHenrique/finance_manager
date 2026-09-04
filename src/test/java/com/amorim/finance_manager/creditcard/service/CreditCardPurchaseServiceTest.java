package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.creditcard.dto.CreateCreditCardPurchaseRequest;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.invoice.service.InvoiceCycle;
import com.amorim.finance_manager.invoice.service.InvoiceCycleService;
import com.amorim.finance_manager.shared.exception.CategoryNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditLimitConflictException;
import com.amorim.finance_manager.shared.exception.IncompatibleCategoryTypeException;
import com.amorim.finance_manager.shared.exception.InvalidCreditCardStatusException;
import com.amorim.finance_manager.shared.exception.InvalidInvoiceStatusException;
import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardPurchaseServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CARD_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID TRANSACTION_ID = UUID.randomUUID();
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2026, 9, 11);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 10, 17);

    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceCycleService invoiceCycleService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private CurrentUserService currentUserService;

    private CreditCardPurchaseService service;

    @BeforeEach
    void setUp() {
        service = new CreditCardPurchaseService(
                creditCardRepository,
                categoryRepository,
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper,
                currentUserService
        );
    }

    @Test
    void shouldCreateACompletedPurchaseAndUpdateCardAndInvoiceWithoutAccounts() {
        CreateCreditCardPurchaseRequest request = request("100.00");
        CreditCard card = card(CreditCardStatus.ACTIVE, "100.00");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        InvoiceCycle cycle = cycle();
        Invoice invoice = invoice(InvoiceStatus.OPEN, "20.00");
        TransactionResponse response = mock(TransactionResponse.class);

        stubOwnedResources(card, category, cycle);
        when(creditCardRepository.saveAndFlush(card)).thenReturn(card);
        when(invoiceCycleService.findOrCreate(card, cycle)).thenReturn(invoice);
        when(invoiceRepository.saveAndFlush(invoice)).thenReturn(invoice);
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(TRANSACTION_ID);
                    return transaction;
                });
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(response);

        TransactionResponse result = service.create(CARD_ID, request);

        assertThat(result).isSameAs(response);
        assertThat(card.getAvailableLimit()).isEqualByComparingTo("0.00");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("120.00");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).saveAndFlush(captor.capture());
        Transaction transaction = captor.getValue();

        assertThat(transaction.getId()).isEqualTo(TRANSACTION_ID);
        assertThat(transaction.getUserId()).isEqualTo(USER_ID);
        assertThat(transaction.getDescription()).isEqualTo("Supermercado");
        assertThat(transaction.getAmount()).isEqualByComparingTo("100.00");
        assertThat(transaction.getCompetenceDate()).isEqualTo(PURCHASE_DATE);
        assertThat(transaction.getEffectiveDate()).isNull();
        assertThat(transaction.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT_CARD_PURCHASE);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(transaction.getSourceAccountId()).isNull();
        assertThat(transaction.getDestinationAccountId()).isNull();
        assertThat(transaction.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(transaction.getCreditCardId()).isEqualTo(CARD_ID);
        assertThat(transaction.getInvoiceId()).isEqualTo(INVOICE_ID);
        assertThat(transaction.getInstallmentGroupId()).isNull();
        assertThat(transaction.getInstallmentNumber()).isEqualTo(1);
        assertThat(transaction.getInstallmentCount()).isEqualTo(1);

        InOrder persistenceOrder = inOrder(
                creditCardRepository,
                invoiceCycleService,
                invoiceRepository,
                transactionRepository
        );
        persistenceOrder.verify(invoiceCycleService).calculate(PURCHASE_DATE, 10, 17);
        persistenceOrder.verify(creditCardRepository).saveAndFlush(card);
        persistenceOrder.verify(invoiceCycleService).findOrCreate(card, cycle);
        persistenceOrder.verify(invoiceRepository).saveAndFlush(invoice);
        persistenceOrder.verify(transactionRepository).saveAndFlush(transaction);
    }

    @Test
    void shouldHideMissingOrForeignCardsAndStopBeforeReadingTheCategory() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00")))
                .isInstanceOf(CreditCardNotFoundException.class);

        verifyNoInteractions(
                categoryRepository,
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
    }

    @ParameterizedTest
    @EnumSource(value = CreditCardStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectCardsThatAreNotActive(CreditCardStatus status) {
        CreditCard card = card(status, "500.00");
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00")))
                .isInstanceOf(InvalidCreditCardStatusException.class);

        verifyNoInteractions(
                categoryRepository,
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
        verify(creditCardRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldHideMissingOrForeignCategoriesAndLeaveTheCardUntouched() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00")))
                .isInstanceOf(CategoryNotFoundException.class);

        assertThat(card.getAvailableLimit()).isEqualByComparingTo("500.00");
        verify(creditCardRepository, never()).saveAndFlush(any());
        verifyNoInteractions(
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
    }

    @Test
    void shouldRejectAnIncomeCategory() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        Category category = category(CategoryType.INCOME, CategoryStatus.ACTIVE);
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00")))
                .isInstanceOf(IncompatibleCategoryTypeException.class);

        verify(creditCardRepository, never()).saveAndFlush(any());
        verifyNoInteractions(invoiceRepository, invoiceCycleService, transactionRepository, transactionMapper);
    }

    @Test
    void shouldRejectAnInactiveExpenseCategory() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.INACTIVE);
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00")))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Categoria inativa não pode receber novas compras");

        verify(creditCardRepository, never()).saveAndFlush(any());
        verifyNoInteractions(invoiceRepository, invoiceCycleService, transactionRepository, transactionMapper);
    }

    @Test
    void shouldRejectInsufficientLimitBeforeCreatingAnInvoiceOrTransaction() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "99.99");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        InvoiceCycle cycle = cycle();
        stubOwnedResources(card, category, cycle);

        assertThatThrownBy(() -> service.create(CARD_ID, request("100.00")))
                .isInstanceOf(CreditLimitConflictException.class)
                .hasMessage("Limite disponível insuficiente para realizar a compra");

        assertThat(card.getAvailableLimit()).isEqualByComparingTo("99.99");
        verify(creditCardRepository, never()).saveAndFlush(any());
        verify(invoiceCycleService, never()).findOrCreate(
                any(CreditCard.class),
                any(InvoiceCycle.class)
        );
        verifyNoInteractions(invoiceRepository, transactionRepository, transactionMapper);
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = "OPEN", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectAnInvoiceThatIsNotOpen(InvoiceStatus status) {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        InvoiceCycle cycle = cycle();
        Invoice invoice = invoice(status, "20.00");
        stubOwnedResources(card, category, cycle);
        when(creditCardRepository.saveAndFlush(card)).thenReturn(card);
        when(invoiceCycleService.findOrCreate(card, cycle)).thenReturn(invoice);

        assertThatThrownBy(() -> service.create(CARD_ID, request("100.00")))
                .isInstanceOf(InvalidInvoiceStatusException.class);

        verify(invoiceRepository, never()).saveAndFlush(any());
        verifyNoInteractions(transactionRepository, transactionMapper);
    }

    private void stubOwnedResources(
            CreditCard card,
            Category category,
            InvoiceCycle cycle
    ) {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));
        when(invoiceCycleService.calculate(PURCHASE_DATE, 10, 17)).thenReturn(cycle);
    }

    private CreateCreditCardPurchaseRequest request(String amount) {
        return new CreateCreditCardPurchaseRequest(
                "Supermercado",
                new BigDecimal(amount),
                PURCHASE_DATE,
                CATEGORY_ID
        );
    }

    private CreditCard card(CreditCardStatus status, String availableLimit) {
        CreditCard card = new CreditCard();
        card.setId(CARD_ID);
        card.setUserId(USER_ID);
        card.setCreditLimit(new BigDecimal("1000.00"));
        card.setAvailableLimit(new BigDecimal(availableLimit));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setStatus(status);
        card.setVersion(0L);
        return card;
    }

    private Category category(CategoryType type, CategoryStatus status) {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setUserId(USER_ID);
        category.setType(type);
        category.setStatus(status);
        return category;
    }

    private InvoiceCycle cycle() {
        return new InvoiceCycle(
                10,
                2026,
                LocalDate.of(2026, 10, 10),
                DUE_DATE
        );
    }

    private Invoice invoice(InvoiceStatus status, String totalAmount) {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setCreditCardId(CARD_ID);
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setDueDate(DUE_DATE);
        invoice.setStatus(status);
        invoice.setVersion(0L);
        return invoice;
    }
}
