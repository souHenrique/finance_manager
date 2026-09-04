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

    @Transactional
    public TransactionResponse create(
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

        InvoiceCycle cycle = invoiceCycleService.calculate(
                request.purchaseDate(),
                card.getClosingDay(),
                card.getDueDay()
        );

        consumeLimit(card, request);

        creditCardRepository.saveAndFlush(card);

        Invoice invoice = invoiceCycleService.findOrCreate(card, cycle);

        if (invoice.getStatus() != InvoiceStatus.OPEN) {
            throw new InvalidInvoiceStatusException();
        }

        invoice.setTotalAmount(
                invoice.getTotalAmount().add(request.amount())
        );
        invoiceRepository.saveAndFlush(invoice);

        Transaction transaction = buildTransaction(
                userId,
                card,
                invoice,
                request
        );

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        log.info(
                """
                event=credit_card.purchase_created \
                transactionId={} creditCardId={} invoiceId={} userId={}
                """,
                saved.getId(),
                card.getId(),
                invoice.getId(),
                userId
        );

        return transactionMapper.toResponse(saved);
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
            CreateCreditCardPurchaseRequest request
    ) {
        Transaction transaction = new Transaction();

        transaction.setUserId(userId);
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());

        transaction.setCompetenceDate(request.purchaseDate());
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

        transaction.setInstallmentGroupId(null);
        transaction.setInstallmentNumber(1);
        transaction.setInstallmentCount(1);

        return transaction;
    }
}
