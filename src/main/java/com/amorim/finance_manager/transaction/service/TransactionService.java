package com.amorim.finance_manager.transaction.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.*;
import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final CurrentUserService currentUserService;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        validateRequest(request);

        Category category = findOwnedCategory(request.categoryId(), userId);

        validateCategory(category, request.type());

        UUID accountId = resolveAccountId(request);

        if (request.status() == TransactionStatus.PENDING) {
            validateOwnedActiveAccount(accountId, userId);
        } else {
            applyBalance(request, userId, accountId);
        }

        Transaction transaction = transactionMapper.toEntity(request);

        transaction.setUserId(userId);

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID transactionId) {
        UUID userId = currentUserService.getCurrentUserId();

        Transaction transaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(TransactionNotFoundException::new);

        return transactionMapper.toResponse(transaction);
    }

    private void applyBalance(CreateTransactionRequest request, UUID userId, UUID accountId) {
        if (request.type() == TransactionType.INCOME) {
            accountBalanceService.credit(userId, accountId, request.amount());
            return;
        }

        accountBalanceService.debit(userId, accountId, request.amount());
    }

    private Category findOwnedCategory(UUID categoryId, UUID userId) {
        return categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private void validateCategory(Category category, TransactionType transactionType) {
        CategoryType expectedType =
                transactionType == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;

        if (category.getType() != expectedType) {
            throw new IncompatibleCategoryTypeException();
        }

        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new InvalidTransactionException("Categoria inativa não pode receber novas transações");
        }
    }

    private void validateOwnedActiveAccount(UUID accountId, UUID userId) {
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        account.ensureActive();
    }

    private UUID resolveAccountId(CreateTransactionRequest request) {
        if (request.type() == TransactionType.INCOME) {
            return request.destinationAccountId();
        }

        return request.sourceAccountId();
    }

    private void validateRequest(CreateTransactionRequest request) {
        if (request.type() != TransactionType.INCOME && request.type() != TransactionType.EXPENSE) {
            throw new InvalidTransactionException("Apenas receitas e despensas são suportadas nesta operação");
        }

        if (request.paymentMethod() == PaymentMethod.CREDIT_CARD) {
            throw new InvalidTransactionException("Transações de cartão de crédito ainda não são suportadas");
        }

        if (request.status() == TransactionStatus.CANCELLED) {
            throw new InvalidTransactionException("Uma transação não pode ser criada como cancelada");
        }

        validateDates(request);
        validateAccounts(request);
        validateFutureFields(request);
    }

    private void validateDates(CreateTransactionRequest request) {
        if (request.status() == TransactionStatus.COMPLETED && request.effectiveDate() == null) {
            throw new InvalidTransactionException("Transação concluída deve possuir data efetiva");
        }

        if (request.status() == TransactionStatus.PENDING && request.effectiveDate() != null) {
            throw new InvalidTransactionException("Transação pendente não deve possuir data efetiva");
        }
    }

    private void validateAccounts(CreateTransactionRequest request) {
        if (request.type() == TransactionType.INCOME) {
            if (request.destinationAccountId() == null) {
                throw new InvalidTransactionException("Receita deve possuir conta de destino");
            }

            if (request.sourceAccountId() != null) {
                throw new InvalidTransactionException("Receita não deve possuir conta de origem");
            }

            return;
        }

        if (request.sourceAccountId() == null) {
            throw new InvalidTransactionException("Despesa deve possuir conta de origem");
        }

        if (request.destinationAccountId() != null) {
            throw new InvalidTransactionException("Despesa não deve possuir conta de destino");
        }
    }

    private void validateFutureFields(CreateTransactionRequest request) {
        if (request.creditCardId() != null
                || request.invoiceId() != null
                || request.installmentGroupId() != null
                || request.installmentNumber() != null
                || request.installmentCount() != null) {
            throw new InvalidTransactionException("Cartão, fatura e parcelamento ainda não suportados");
        }
    }
}
