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
import com.amorim.finance_manager.shared.exception.*;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.*;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardPurchaseService {

    private final CreditCardRepository creditCardRepository;
    private final CategoryRepository categoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceCycleService invoiceCycleService;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final CurrentUserService currentUserService;
    private final InstallmentCalculator installmentCalculator;

    @Transactional
    public List<TransactionResponse> create(
            UUID creditCardId,
            CreateCreditCardPurchaseRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        CreditCard card = creditCardRepository
                .findByIdAndUserId(creditCardId, userId)
                .orElseThrow(CreditCardNotFoundException::new);
        validateCard(card);

        Category category = categoryRepository
                .findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(CategoryNotFoundException::new);

        validateCategory(category);

        List<BigDecimal> installmentAmounts = installmentCalculator.split(
                request.amount(),
                request.installmentCount()
        );

        InvoiceCycle initialCycle = invoiceCycleService.calculate(
                request.purchaseDate(),
                card.getClosingDay(),
                card.getDueDay()
        );

        consumeLimit(card, request);
        creditCardRepository.saveAndFlush(card);

        UUID installmentGroupId = UUID.randomUUID();

        List<Transaction> transactions =
                new ArrayList<>(request.installmentCount());

        for (int index = 0; index < installmentAmounts.size(); index++) {
            int installmentNumber = index + 1;

            BigDecimal installmentAmount =
                    installmentAmounts.get(index);

            InvoiceCycle installmentCycle =
                    invoiceCycleService.shift(
                            initialCycle,
                            index,
                            card.getClosingDay(),
                            card.getDueDay()
                    );

            Invoice invoice = invoiceCycleService.findOrCreate(
                    card,
                    installmentCycle
            );

            if (invoice.getStatus() != InvoiceStatus.OPEN) {
                throw new InvalidInvoiceStatusException();
            }

            invoice.setTotalAmount(
                    invoice.getTotalAmount().add(installmentAmount)
            );

            invoiceRepository.saveAndFlush(invoice);

            Transaction transaction = buildTransaction(
                    userId,
                    card,
                    invoice,
                    request,
                    installmentGroupId,
                    installmentNumber,
                    request.installmentCount(),
                    installmentAmount,
                    request.purchaseDate().plusMonths(index)
            );

            transactions.add(transaction);
        }

        List<Transaction> savedTransactions =
                transactionRepository.saveAllAndFlush(transactions);

        log.info(
                """
                event=credit_card.installment_purchase_created \
                installmentGroupId={} creditCardId={} \
                installmentCount={} userId={}
                """,
                installmentGroupId,
                card.getId(),
                request.installmentCount(),
                userId
        );

        return savedTransactions
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    private void validateCard(CreditCard card) {
        if (card.getStatus() != CreditCardStatus.ACTIVE) {
            throw new InvalidCreditCardStatusException();
        }
    }

    private void validateCategory(Category category) {
        if (category.getType() != CategoryType.EXPENSE) {
            throw new IncompatibleCategoryTypeException();
        }

        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new InvalidTransactionException(
                    "Categoria inativa não pode receber novas compras"
            );
        }
    }

    private void consumeLimit(
            CreditCard card,
            CreateCreditCardPurchaseRequest request
    ) {
        if (card.getAvailableLimit().compareTo(request.amount()) < 0) {
            throw new CreditLimitConflictException(
                    "Limite disponível insuficiente para realizar a compra"
            );
        }

        card.setAvailableLimit(
                card.getAvailableLimit().subtract(request.amount())
        );
    }

    private Transaction buildTransaction(
            UUID userId,
            CreditCard card,
            Invoice invoice,
            CreateCreditCardPurchaseRequest request,
            UUID installmentGroupId,
            int installmentNumber,
            int installmentCount,
            BigDecimal installmentAmount,
            LocalDate competenceDate
    ) {
        Transaction transaction = new Transaction();

        transaction.setUserId(userId);
        transaction.setDescription(request.description());
        transaction.setAmount(installmentAmount);

        transaction.setCompetenceDate(competenceDate);
        transaction.setEffectiveDate(null);
        transaction.setDueDate(invoice.getDueDate());

        transaction.setType(TransactionType.CREDIT_CARD_PURCHASE);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        transaction.setSourceAccountId(null);
        transaction.setDestinationAccountId(null);

        transaction.setCategoryId(request.categoryId());
        transaction.setCreditCardId(card.getId());
        transaction.setInvoiceId(invoice.getId());

        transaction.setInstallmentGroupId(installmentGroupId);
        transaction.setInstallmentNumber(installmentNumber);
        transaction.setInstallmentCount(installmentCount);

        return transaction;
    }
}
