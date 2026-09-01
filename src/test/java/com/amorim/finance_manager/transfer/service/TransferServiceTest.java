package com.amorim.finance_manager.transfer.service;

import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.InactiveAccountException;
import com.amorim.finance_manager.shared.exception.InvalidTransferException;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.transfer.dto.CreateTransferRequest;
import com.amorim.finance_manager.transfer.mapper.TransferMapper;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID SOURCE_ACCOUNT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID DESTINATION_ACCOUNT_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final BigDecimal AMOUNT =
            new BigDecimal("40.00");

    private static final LocalDate DATE =
            LocalDate.of(2026, 9, 1);

    @Mock
    private AccountBalanceService accountBalanceService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TransferService transferService;

    @Test
    void shouldDebitSourceCreditDestinationAndPersistTransferInOrder() {
        CreateTransferRequest request = validRequest();
        Transaction transaction = transferTransaction();
        TransactionResponse response = transferResponse();

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(transferMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponse result = transferService.transfer(request);

        InOrder inOrder = inOrder(accountBalanceService, transactionRepository);
        inOrder.verify(accountBalanceService)
                .debit(USER_ID, SOURCE_ACCOUNT_ID, AMOUNT);
        inOrder.verify(accountBalanceService)
                .credit(USER_ID, DESTINATION_ACCOUNT_ID, AMOUNT);
        inOrder.verify(transactionRepository)
                .saveAndFlush(transaction);

        assertThat(transaction.getUserId()).isEqualTo(USER_ID);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldRejectTransferBetweenSameAccount() {
        CreateTransferRequest request = new CreateTransferRequest(
                SOURCE_ACCOUNT_ID,
                SOURCE_ACCOUNT_ID,
                AMOUNT,
                DATE,
                "Same account transfer"
        );

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("As contas de origem e destino devem ser diferentes");

        verifyNoInteractions(
                currentUserService,
                accountBalanceService,
                transferMapper,
                transactionRepository,
                transactionMapper
        );
    }

    @Test
    void shouldRejectNullZeroAndNegativeAmount() {
        CreateTransferRequest nullAmount = requestWithAmount(null);
        CreateTransferRequest zeroAmount = requestWithAmount(BigDecimal.ZERO);
        CreateTransferRequest negativeAmount = requestWithAmount(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> transferService.transfer(nullAmount))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("O valor da transferência deve ser maior que zero");

        assertThatThrownBy(() -> transferService.transfer(zeroAmount))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("O valor da transferência deve ser maior que zero");

        assertThatThrownBy(() -> transferService.transfer(negativeAmount))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("O valor da transferência deve ser maior que zero");

        verifyNoInteractions(
                currentUserService,
                accountBalanceService,
                transferMapper,
                transactionRepository,
                transactionMapper
        );
    }

    @Test
    void shouldNotCreditOrPersistWhenSourceDebitFails() {
        CreateTransferRequest request = validRequest();

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        doThrow(new AccountNotFoundException())
                .when(accountBalanceService)
                .debit(USER_ID, SOURCE_ACCOUNT_ID, AMOUNT);

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountBalanceService, never()).credit(any(), any(), any());
        verifyNoInteractions(transferMapper, transactionRepository, transactionMapper);
    }

    @Test
    void shouldNotPersistWhenDestinationCreditFailsAfterDebit() {
        CreateTransferRequest request = validRequest();

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        doThrow(new InactiveAccountException())
                .when(accountBalanceService)
                .credit(USER_ID, DESTINATION_ACCOUNT_ID, AMOUNT);

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InactiveAccountException.class);

        InOrder inOrder = inOrder(accountBalanceService);
        inOrder.verify(accountBalanceService)
                .debit(USER_ID, SOURCE_ACCOUNT_ID, AMOUNT);
        inOrder.verify(accountBalanceService)
                .credit(USER_ID, DESTINATION_ACCOUNT_ID, AMOUNT);

        verifyNoInteractions(transferMapper, transactionRepository, transactionMapper);
    }

    private CreateTransferRequest validRequest() {
        return requestWithAmount(AMOUNT);
    }

    private CreateTransferRequest requestWithAmount(BigDecimal amount) {
        return new CreateTransferRequest(
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                amount,
                DATE,
                "Transfer test"
        );
    }

    private Transaction transferTransaction() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setDescription("Transfer test");
        transaction.setAmount(AMOUNT);
        transaction.setCompetenceDate(DATE);
        transaction.setEffectiveDate(DATE);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.TRANSFER);
        transaction.setSourceAccountId(SOURCE_ACCOUNT_ID);
        transaction.setDestinationAccountId(DESTINATION_ACCOUNT_ID);
        return transaction;
    }

    private TransactionResponse transferResponse() {
        return new TransactionResponse(
                TRANSACTION_ID,
                "Transfer test",
                AMOUNT,
                DATE,
                DATE,
                null,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                PaymentMethod.TRANSFER,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
